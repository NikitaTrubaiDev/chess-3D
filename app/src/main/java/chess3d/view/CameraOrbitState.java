package chess3d.view;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.input.ChaseCamera;
import com.jme3.input.InputManager;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.math.FastMath;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;

public class CameraOrbitState extends BaseAppState {
    private static final String ZOOM_IN  = "orbit.zoomIn";
    private static final String ZOOM_OUT = "orbit.zoomOut";

    private final Node pivot;

    private Camera cam;
    private InputManager input;
    private ChaseCamera chaseCam;

    // дистанции
    private float defaultDist, minDist, maxDist;

    // плавный зум
    private float targetDist, currentDist;
    private float zoomStepBase;            // базовый шаг от диапазона
    private AnalogListener zoomListener;

    public CameraOrbitState(Node pivot, float defaultDist, float minDist, float maxDist) {
        this.pivot = pivot;
        this.defaultDist = defaultDist;
        this.minDist = minDist;
        this.maxDist = maxDist;
    }

    @Override
    protected void initialize(Application app) {
        SimpleApplication sapp = (SimpleApplication) app;
        cam   = sapp.getCamera();
        input = sapp.getInputManager();

        // выключаем flyCam, включаем курсор
        sapp.getFlyByCamera().setEnabled(false);
        input.setCursorVisible(true);

        // создаём ChaseCamera, встроенный зум отключаем
        chaseCam = new ChaseCamera(cam, pivot, input);
        chaseCam.setToggleRotationTrigger(new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));
        chaseCam.setSmoothMotion(false);
        chaseCam.setRotationSpeed(5.0f);
        chaseCam.setZoomSensitivity(0f);   // свой зум

        chaseCam.setMinVerticalRotation(-FastMath.HALF_PI + 0.05f);
        chaseCam.setMaxVerticalRotation( FastMath.HALF_PI - 0.05f);
        chaseCam.setDefaultHorizontalRotation(0.25f * FastMath.PI);
        chaseCam.setDefaultVerticalRotation(50f * FastMath.DEG_TO_RAD);

        // дистанции
        chaseCam.setMinDistance(minDist);
        chaseCam.setMaxDistance(maxDist);
        chaseCam.setDefaultDistance(defaultDist);
        currentDist = targetDist = FastMath.clamp(defaultDist, minDist, maxDist);

        // базовый мягкий шаг (2–3% диапазона)
        zoomStepBase = Math.max((maxDist - minDist) * 0.025f, defaultDist * 0.02f);

        // страхуемся от старых мэппингов
        if (input.hasMapping(ZOOM_IN))  input.deleteMapping(ZOOM_IN);
        if (input.hasMapping(ZOOM_OUT)) input.deleteMapping(ZOOM_OUT);

        // ось колёсика (оба направления)
        input.addMapping(ZOOM_IN,  new MouseAxisTrigger(MouseInput.AXIS_WHEEL, false));
        input.addMapping(ZOOM_OUT, new MouseAxisTrigger(MouseInput.AXIS_WHEEL, true));

        zoomListener = (name, value, tpf) -> {
            // динамический шаг: чуть больше, когда далеко, и очень мелкий рядом с доской
            float step = Math.max(zoomStepBase, currentDist * 0.03f); // 0.03–0.05 — больше/меньше

            if (ZOOM_IN.equals(name))  targetDist -= step * value;
            if (ZOOM_OUT.equals(name)) targetDist += step * value;

            targetDist = FastMath.clamp(targetDist, minDist, maxDist);
        };
        input.addListener(zoomListener, ZOOM_IN, ZOOM_OUT);
    }

    @Override
    public void update(float tpf) {
        // экспоненциальное сглаживание без «длинного хвоста»
        final float halfLife = 0.10f;
        float s     = FastMath.pow(0.5f, tpf / halfLife);
        float alpha = 1f - s;

        float next = currentDist + (targetDist - currentDist) * alpha;
        next = FastMath.clamp(next, minDist, maxDist);   // жёсткий кламп всегда

        chaseCam.setDefaultDistance(next);
        currentDist = next;
    }

    /** Позволяет из App переустановить диапазон после того, как станет известен размер сцены. */
    public void setDistances(float def, float min, float max) {
        this.defaultDist = def;
        this.minDist = min;
        this.maxDist = max;

        // пересчитываем текущее и базовый шаг
        this.currentDist = this.targetDist = FastMath.clamp(def, min, max);
        chaseCam.setMinDistance(min);
        chaseCam.setMaxDistance(max);
        chaseCam.setDefaultDistance(currentDist);

        zoomStepBase = Math.max((max - min) * 0.025f, def * 0.02f);
    }

    @Override
    protected void cleanup(Application app) {
        if (input != null) {
            if (input.hasMapping(ZOOM_IN))  input.deleteMapping(ZOOM_IN);
            if (input.hasMapping(ZOOM_OUT)) input.deleteMapping(ZOOM_OUT);
            if (zoomListener != null)       input.removeListener(zoomListener);
        }
    }

    @Override protected void onEnable() {}
    @Override protected void onDisable() {}
}
