package com.cgm.java.console.commands.implementations;

import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.collections.ListUtils;

import com.cgm.java.console.commands.BridgeCommand;
import com.cgm.java.hue.models.Light;
import com.cgm.java.hue.models.State;
import com.cgm.java.hue.utilities.HueBridgePutter;
import com.cgm.java.utilities.lambdas.Conversion;

/**
 * A command to turn on lights
 */
public class ToggleLight extends BridgeCommand {
    private static final String NAME_OPTION = "name";
    private static final String ID_OPTION = "id";

    @Override
    public void usage() {
        final HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(getName() + " <bridge IP> <hue ID> <light name>", "A command to turn on a specific light.", getOptions(), null);
    }

    @Override
    protected int run(final CommandLine line) throws RuntimeException, UnknownHostException {
        setIpAndId(line);
        final String[] lightNames = line.hasOption(NAME_OPTION) ? line.getOptionValues(NAME_OPTION) : new String[] {};
        final String[] lightIds = line.hasOption(ID_OPTION) ? line.getOptionValues(ID_OPTION) : new String[] {};
        if ((lightNames == null || lightNames.length < 1) && (lightIds == null || lightIds.length < 1)) {
            usage();
            throw new IllegalArgumentException("You must specify at least one light to turn on.");
        }

        // Collect all lights and keep them in a map
        final List<Light> lights = HUE_BRIDGE_GETTER.getLights(bridgeIp, hueId);
        final Map<String, Light> nameToLightMap = lights.stream().collect(Collectors.toMap(Conversion.LIGHT_TO_NAME, (light) -> light));
        final Map<String, Light> idToLightMap = lights.stream().collect(Collectors.toMap(Conversion.LIGHT_TO_ID, (light) -> light));
        System.out.println("These lights were found: ");
        nameToLightMap.keySet().forEach(System.out::println);
        System.out.println();

        // Figure out which requested lights actually match the available lights, and turn them on
        final HueBridgePutter hueBridgePutter = new HueBridgePutter();
        final List<String> lightNamesToggle = Arrays.stream(lightNames).filter(nameToLightMap::containsKey).collect(Collectors.toList());
        final List<String> lightIdsToToggle = Arrays.stream(lightIds).filter(idToLightMap::containsKey).collect(Collectors.toList());
        final List<String> union = ListUtils.union(lightNamesToggle, lightIdsToToggle);
        System.out.println("These lights matched your input: ");
        union.forEach((lightIdentifier) -> {
            System.out.println(lightIdentifier);
            Light light = nameToLightMap.get(lightIdentifier);
            if(light==null){
                light = idToLightMap.get(lightIdentifier);
            }

            final State newState = extractAndToggleState(light);

            hueBridgePutter.setLightState(bridgeIp, hueId, light.getId().toString(), newState);
        });

        return 0;
    }

    private State extractAndToggleState(final Light light) {
        final State oldState = light.getState();
        final State.Builder newStateBuilder = State.newBuilder(oldState).setOn(!oldState.getOn());
        return newStateBuilder.build();
    }

    @Override
    public String getName() {
        return "togglelight";
    }

    @Override
    public Options getOptions() {
        final Options options = super.getOptions();
        options.addOption(NAME_OPTION, true, "The name of the light to toggle.");
        options.addOption(ID_OPTION, true, "The name of the light to toggle.");
        return options;
    }
}