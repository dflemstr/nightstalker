package name.dflemstr.nightstalker;

import com.google.auto.value.AutoValue;

@AutoValue
abstract class Hero {
    abstract int id();

    abstract String name();

    abstract String localizedName();

    public static Hero create(int id, String name, String localizedName) {
        return new AutoValue_Hero(id, name, localizedName);
    }
}
