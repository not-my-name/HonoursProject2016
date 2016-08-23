package za.redbridge.simulator;

import org.jbox2d.dynamics.World;
import org.jbox2d.common.Vec2;
import za.redbridge.simulator.object.ResourceObject;
import za.redbridge.simulator.config.SchemaConfig;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import sim.engine.Steppable;
import org.jbox2d.dynamics.joints.Joint;
import org.jbox2d.dynamics.joints.JointDef;
import org.jbox2d.dynamics.joints.WeldJointDef;
import sim.engine.SimState;

/*
 *  The construction task class
 *
 */

public class ConstructionTask implements Steppable{
    private ArrayList<ResourceObject> resources;
    private SchemaConfig schema;
    private HashMap<ResourceObject, ArrayList<ResourceObject>> weldMap;
    private World physicsWorld;

    public ConstructionTask(String path, ArrayList<ResourceObject> r, World world){
        schema = new SchemaConfig(path,1,3);
        resources = r;
        weldMap = new HashMap<ResourceObject, ArrayList<ResourceObject>>();
        for(int i=0;i<resources.size();i++){
            ArrayList<ResourceObject> temp = new ArrayList<ResourceObject>();
            weldMap.put(resources.get(i), temp);
        }
        physicsWorld = world;
        update();
    }

    public ConstructionTask(String path, World world){
        schema = new SchemaConfig("configs/schemaConfig.yml",1,3);
        physicsWorld = world;
        weldMap = new HashMap<ResourceObject, ArrayList<ResourceObject>>();
    }

    public void addResources(ArrayList<ResourceObject> r){
        resources = r;
        for(ResourceObject resource : resources){
            resource.updateAdjacent(resources);
        }
        for(int i=0;i<resources.size();i++){
            ArrayList<ResourceObject> temp = new ArrayList<ResourceObject>();
            weldMap.put(resources.get(i), temp);
        }
        checkPotentialWeld(r.get(0), r.get(1));
    }

    @Override
    public void step(SimState simState) {
        for(ResourceObject firstR : resources){
            for(ResourceObject secondR : resources){
                if(firstR != secondR){
                    // check if join between resources has been made before
                    boolean t = false;
                    for(int i=0;i<weldMap.get(firstR).size();i++){
                        if(weldMap.get(firstR).get(i)==secondR){
                            t = true;
                            break;
                        }
                    }
                    float distance = firstR.getBody().getPosition().sub(secondR.getBody().getPosition()).length();
                    if(distance < 3f && t==false){
                        // if(checkPotentialWeld(firstR, secondR)){
                        //     Joint joint = physicsWorld.createJoint(createWeld(firstR, secondR));
                        //     weldMap.get(firstR).add(secondR);
                        //     weldMap.get(secondR).add(firstR);
                        //     System.out.println("Create weld");
                        // }
                    }
                }
            }
        }
    }

    public boolean checkPotentialWeld(ResourceObject r1, ResourceObject r2){
        if(r1.checkPotentialWeld(r2) || r2.checkPotentialWeld(r1)){
            return true;
        }
        return false;
    }

    private WeldJointDef createWeld(ResourceObject r1, ResourceObject r2){
        WeldJointDef wjd = new WeldJointDef();
        wjd.bodyA = r1.getBody();
        wjd.bodyB = r2.getBody();
        wjd.localAnchorA.set(wjd.bodyA.getPosition());
        wjd.localAnchorB.set(wjd.bodyB.getPosition());
        wjd.collideConnected = true;
        return wjd;
    }

    public void update(ArrayList<ResourceObject> r){
        resources = r;
        for(ResourceObject resource : resources){
            resource.updateAdjacent(resources);
        }
    }

    public void update(){
        for(ResourceObject resource : resources){
            resource.updateAdjacent(resources);
        }
    }

    public void printConnected(){
        for(ResourceObject resource : resources){
            String [] adjacentResources = resource.getAdjacentResources();
            for(int i=0;i<adjacentResources.length;i++){
                System.out.print(adjacentResources[i]+" ");
            }
            System.out.println();
        }
    }

    public int checkSchema(int i){
        int correct = 0;
        for(ResourceObject resource : resources){
            if(schema.checkConfig(i,resource.getType(), resource.getAdjacentResources())){
                System.out.print("Resource "+resource.getType()+" is correct -> ");
                for(int j=0;j<resource.getAdjacentResources().length;j++){
                    System.out.print(resource.getAdjacentResources()[j]+" ");
                }
                System.out.println();
                correct++;
            }
        }
        return correct;
    }

    public int[] configResQuantity(int i){
        return schema.getResQuantity(i);
    }
}
