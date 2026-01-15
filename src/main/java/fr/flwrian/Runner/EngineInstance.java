package fr.flwrian.Runner;

import fr.flwrian.Engine.Engine;

/**
 * Represents an engine instance with its name.
 */
public class EngineInstance {
    private final Engine engine;
    private final String name;

    public EngineInstance(Engine engine, String name) {
        this.engine = engine;
        this.name = name;
    }

    public Engine getEngine() {
        return engine;
    }

    public String getName() {
        return name;
    }
}
