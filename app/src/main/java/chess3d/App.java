package chess3d;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.bounding.BoundingBox;
import com.jme3.math.ColorRGBA;
import com.jme3.system.AppSettings;

import chess3d.view.CameraOrbitState;
import chess3d.view.GameSceneState;
import chess3d.view.LightingState;

public class App extends SimpleApplication {

    private GameSceneState game;
    private CameraOrbitState orbit;
    private boolean orbitConfigured = false;

    @Override
    public void simpleInitApp() {
        assetManager.registerLocator("assets", FileLocator.class);
        viewPort.setBackgroundColor(ColorRGBA.White);

        var pivot = new com.jme3.scene.Node("pivot");
        rootNode.attachChild(pivot);

        // 1) Сцена (доска + фигуры)
        game = new GameSceneState(pivot);
        stateManager.attach(game);

        // 2) Свет — как раньше
        stateManager.attach(new LightingState());

        // 3) Орбитальная камера — временные дистанции, потом переустановим
        orbit = new CameraOrbitState(pivot, 10f, 1f, 100f);
        stateManager.attach(orbit);
    }

    @Override
    public void simpleUpdate(float tpf) {
        if (!orbitConfigured) {
            BoundingBox bb = game.getSceneBounds();
            if (bb != null) {
                float maxExtent = Math.max(Math.max(bb.getXExtent(), bb.getYExtent()), bb.getZExtent());

                float aspect = (float) cam.getWidth() / (float) cam.getHeight();
                float near   = Math.max(0.01f, maxExtent * 0.004f); // ближе к доске
                float far    = maxExtent * 35f;                      // поменьше дальняя
                cam.setFrustumPerspective(45f, aspect, near, far);

                float def = maxExtent * 2.8f;                        // стартовая
                float min = Math.max(near * 1.2f, maxExtent * 0.08f);// можно подлетать ближе
                float max = maxExtent * 4.0f;                        // не «улетишь» далеко
                orbit.setDistances(def, min, max);

                orbitConfigured = true;
            }

        }
    }

    public static void main(String[] args) {
        App app = new App();
        AppSettings settings = new AppSettings(true);
        settings.setTitle("Chess3D");
        settings.setVSync(true);
        settings.setSamples(4);
        settings.setResizable(true);
        app.setShowSettings(false);
        app.setSettings(settings);
        app.start();
    }
}
