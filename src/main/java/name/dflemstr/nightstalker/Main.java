package name.dflemstr.nightstalker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;
import org.pegdown.Extensions;
import org.pegdown.PegDownProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.processor.runner.SimpleRunner;
import skadistats.clarity.source.MappedFileSource;
import skadistats.clarity.wire.proto.Demo;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;

public class Main {
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);
    private static final String REPORT_TEMPLATE =
            "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"/><link href=\"http://kevinburke.bitbucket.org/markdowncss/markdown.css\" rel=\"stylesheet\"></link></head><body>%s</body></html>";

    public static void main(String[] args) throws IOException {
        String input = args[0];
        MappedFileSource source = new MappedFileSource(input);

        LOG.info("Loading hero info");
        ImmutableSet<Hero> heroes = loadHeroInformation();
        ImmutableMap<Integer, Hero> heroesById = Maps.uniqueIndex(heroes, Hero::id);
        ImmutableMap<String, Hero> heroesByName = Maps.uniqueIndex(heroes, Hero::name);

        LOG.info("Collecting game info");
        GameModelVisitor visitor = new GameModelVisitor();
        new SimpleRunner(source).runWith(visitor);

        LOG.info("Generating report");
        Path inputPath = Paths.get(input);
        Path reportPath = inputPath.resolveSibling(inputPath.getFileName().toString() + "-report.html");
        Files.write(reportPath, generateReport(visitor.fileInfo(), heroesById, heroesByName).getBytes(StandardCharsets.UTF_8));
    }

    static ImmutableSet<Hero> loadHeroInformation() throws IOException {
        ImmutableSet.Builder<Hero> resultBuilder = ImmutableSet.builder();

        JsonNode heroesJson = new ObjectMapper().readTree(
                Resources.toByteArray(Resources.getResource(Main.class, "heroes.json")));
        for (JsonNode node : heroesJson.get("heroes")) {
            resultBuilder.add(
                    Hero.create(
                            node.get("id").asInt(),
                            node.get("name").asText(),
                            node.get("localized_name").asText()));
        }

        return resultBuilder.build();
    }

    static String generateReport(Demo.CDemoFileInfo fileInfo,
                                 ImmutableMap<Integer, Hero> heroesById,
                                 ImmutableMap<String, Hero> heroesByName) {
        Demo.CGameInfo gameInfo = fileInfo.getGameInfo();
        Demo.CGameInfo.CDotaGameInfo dota = gameInfo.getDota();

        Writer markdownBuffer = new StringWriter();
        try (PrintWriter w = new PrintWriter(markdownBuffer)) {
            w.printf("# Replay analysis: *%s* vs *%s*%n", dota.getRadiantTeamTag(), dota.getDireTeamTag());
            w.println();
            writeLineup(w, dota, heroesByName, MessageFormat.format("Radiant ({0})", dota.getRadiantTeamTag()), 2);
            writeLineup(w, dota, heroesByName, MessageFormat.format("Dire ({0})", dota.getDireTeamTag()), 3);
            writePickBans(w, dota, heroesById);
        }
        PegDownProcessor processor = new PegDownProcessor(Extensions.ALL);
        return String.format(REPORT_TEMPLATE, processor.markdownToHtml(markdownBuffer.toString()));
    }

    static void writeLineup(PrintWriter w, Demo.CGameInfo.CDotaGameInfo dota, ImmutableMap<String, Hero> heroesByName, String heading, int teamId) {
        w.println("## " + heading);
        w.println();
        w.println("| Player | Dotabuff | Hero |");
        w.println("|--------|----------|------|");
        for (Demo.CGameInfo.CDotaGameInfo.CPlayerInfo playerInfo : dota.getPlayerInfoList()) {
            if (playerInfo.getGameTeam() == teamId) {
                String name = playerInfo.getHeroName().substring("npc_dota_hero_".length());
                w.printf("| %s | [Link](http://www.dotabuff.com/players/%d) | %s |%n",
                        playerInfo.getPlayerName(),
                        playerInfo.getSteamid(),
                        heroesByName.get(name).localizedName());
            }
        }
        w.println();
    }

    static void writePickBans(PrintWriter w, Demo.CGameInfo.CDotaGameInfo dota, ImmutableMap<Integer, Hero> heroesById) {
        w.println("## Pick/ban order");
        w.println();
        w.println("| Team | Pick/Ban | Hero |");
        w.println("|------|----------|------|");
        for (Demo.CGameInfo.CDotaGameInfo.CHeroSelectEvent pickBan : dota.getPicksBansList()) {
            w.printf("| %s | %s | %s |%n",
                    pickBan.getTeam() == 2 ? "Radiant" : "Dire",
                    pickBan.getIsPick() ? "Pick" : "Ban",
                    heroesById.get(pickBan.getHeroId()).localizedName());
        }
    }

}
