package chess3d.view;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;

public class LightingState extends BaseAppState {
    private DirectionalLight sun, fill, back, up;
    private AmbientLight ambient;

    @Override
    protected void initialize(Application app) {
        var root = ((SimpleApplication) app).getRootNode();

        sun = new DirectionalLight();
        sun.setDirection(new Vector3f(-0.35f, -0.866f, -0.25f).normalizeLocal()); // ~60° сверху
        sun.setColor(ColorRGBA.White.mult(1.0f));
        root.addLight(sun);

        fill = new DirectionalLight();
        fill.setDirection(new Vector3f(0.4f, -0.5f, 0.3f).normalizeLocal());
        fill.setColor(ColorRGBA.White.mult(0.6f));
        root.addLight(fill);

        back = new DirectionalLight();
        back.setDirection(new Vector3f(0f, -0.4f, -1f).normalizeLocal());
        back.setColor(ColorRGBA.White.mult(0.35f));
        root.addLight(back);

        up = new DirectionalLight();
        up.setDirection(new Vector3f(0f, 1f, 0f).normalizeLocal()); // светит снизу вверх up.setColor(ColorRGBA.White.mult(0.10f)); // 0.3–0.45 по вкусу rootNode.addLight(up);
        root.addLight(up);


        ambient = new AmbientLight();
        ambient.setColor(ColorRGBA.White.mult(0.55f)); // «как было» до экспериментов
        root.addLight(ambient);
    }

    
    protected void cleanup(Application app) {
        var root = ((SimpleApplication) app).getRootNode();
        root.removeLight(sun);
        root.removeLight(fill);
        root.removeLight(back);
        root.removeLight(ambient);
    }

     protected void onEnable() {}
     protected void onDisable() {}
}
