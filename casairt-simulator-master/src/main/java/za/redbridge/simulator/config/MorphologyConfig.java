package za.redbridge.simulator.config;

import org.yaml.snakeyaml.Yaml;
import za.redbridge.simulator.config.Config;
import za.redbridge.simulator.sensor.AgentSensor;
import za.redbridge.simulator.Morphology;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by shsu on 2014/09/08.
 */
public class MorphologyConfig extends Config {
    private Map<String,Object> yamlCache;
    private int morphNum;
    private ArrayList<Morphology> morphologyList;

    public MorphologyConfig(String filepath) throws ParseException {
        morphologyList = new ArrayList<Morphology>();
        morphNum = 0;

        Yaml yaml = new Yaml();
        Map<String, Object> config = null;

        try (Reader reader = Files.newBufferedReader(Paths.get(filepath))) {
            config = (Map<String, Object>) yaml.load(reader);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Map meta = (Map) config.get("meta");
        if (checkFieldPresent(meta, "meta")) {
            // Number of morphologies
            Number noMorph = (Number) meta.get("numMorph");
            if (checkFieldPresent(noMorph, "meta:numMorph")) {
                morphNum = noMorph.intValue();
            }
            else {
                throw new ParseException("Error: Number of morphologies not found.", 0);
            }
        }

        for(int i=1;i<=morphNum;i++){
            ArrayList<AgentSensor> sensorList = new ArrayList<>();
            int sensors = 0;
            int totalReadingSize = 0;

            String id = i + "m";
            Map morph = (Map) config.get(id);
            if (checkFieldPresent(morph,id)){
                Map sensorMeta = (Map) morph.get("meta");
                Number noSensors = (Number) sensorMeta.get("numSensors");
                sensors = noSensors.intValue();
            }

            for (int j=1;j<=sensors;j++) {
                String sid = j + "s";
                String type = null;
                float bearing, orientation, fieldOfView, range;

                AgentSensor agentSensor = null;

                Map sensor = (Map) config.get(sid);
                if (checkFieldPresent(sensor, sid)) {
                    Number bear = (Number) sensor.get("bearing");
                    if (checkFieldPresent(bear, sid + ":bearing")) {
                        bearing = bear.floatValue();
                    }
                    else {
                        throw new ParseException("No bearing found for sensor " + sid, j);
                    }

                    Number orient = (Number) sensor.get("orientation");
                    if (checkFieldPresent(bear, sid + ":orientation")) {
                        orientation = orient.floatValue();
                    }
                    else {
                        throw new ParseException("No orientation found for sensor " + sid, j);
                    }

                    Number fov = (Number) sensor.get("fieldOfView");
                    if (checkFieldPresent(bear, sid + ":fieldOfView")) {
                        fieldOfView = fov.floatValue();
                    }
                    else {
                        throw new ParseException("No field of view found for sensor " + sid, j);
                    }

                    Number ran = (Number) sensor.get("range");
                    if (checkFieldPresent(bear, sid + ":range")) {
                        range = ran.floatValue();
                    }
                    else {
                        throw new ParseException("No sensor range found for sensor " + sid, j);
                    }

                    type = (String) sensor.get("type");
                    if (checkFieldPresent(type, sid+":type")) {
                        try {
                            Class sensorType = Class.forName(type.trim());

                            Object o = sensorType.getConstructor(Float.TYPE, Float.TYPE, Float.TYPE, Float.TYPE)
                                    .newInstance((float) Math.toRadians(bearing), (float) Math.toRadians(orientation),
                                            range, (float) Math.toRadians(fieldOfView));

                            if (!(o instanceof AgentSensor)) {
                                throw new InvalidClassException("Not Agent Sensor.");
                            }

                            agentSensor = (AgentSensor) o;
                            // not sure if implemented
                            // agentSensor.readAdditionalConfigs(sensor);
                        }
                        catch (ClassNotFoundException c) {
                            System.out.println("AgentSensor Class not found for " + type);
                            c.printStackTrace();
                            System.exit(-1);
                        }
                        catch (InvalidClassException x) {
                            System.out.println("Invalid specified agent sensor class. " + type);
                            x.printStackTrace();
                            System.exit(-2);
                        }
                        catch (NoSuchMethodException n) {
                            n.printStackTrace();
                        }
                        catch (InvocationTargetException inv) {
                            inv.getCause();
                            inv.printStackTrace();
                        }
                        catch (InstantiationException ins) {
                            ins.printStackTrace();
                        }
                        catch (IllegalAccessException ill) {
                            ill.printStackTrace();
                        }

                    }
                    else {
                        throw new ParseException("Error: No sensor type found. ", j);
                    }
                }
                else {
                    throw new ParseException("Error: " + j + " Sensor not found.", j);
                }
                sensorList.add(agentSensor);
            }
            Morphology morphology = new Morphology(sensorList, sensors);
            morphologyList.add(morphology);
        }

        yamlCache = config;
    }

    public int getMorphNumber(){
        return morphNum;
    }

    public Morphology getMorphology(int i){
        return morphologyList.get(i);
    }
}
