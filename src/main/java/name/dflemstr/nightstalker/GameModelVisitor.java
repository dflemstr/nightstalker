package name.dflemstr.nightstalker;

import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.wire.proto.Demo;

public class GameModelVisitor {

    private Demo.CDemoFileInfo fileInfo;

    public Demo.CDemoFileInfo fileInfo() {
        return fileInfo;
    }

    @OnMessage(Demo.CDemoFileInfo.class)
    public void onMessage(Context ctx, Demo.CDemoFileInfo fileInfo) {
        this.fileInfo = fileInfo;
    }
}
