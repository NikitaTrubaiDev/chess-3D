package chess3d.view;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.input.InputManager;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Ray;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Torus;

public class PickingState extends BaseAppState {

    private SimpleApplication app;
    private InputManager input;
    private Camera cam;
    private Node root;

    private GameSceneState game;
    private Spatial selectedPiece;
    private Geometry selectionRing;

    private final ActionListener clickListener = new ActionListener() {
        @Override public void onAction(String name, boolean isPressed, float tpf) {
            if (!isEnabled()) return;
            if ("Pick".equals(name) && !isPressed) { // ЛКМ отпущена
                pickAtCursor();
            }
        }
    };

    @Override protected void initialize(Application _app) {
        this.app = (SimpleApplication)_app;
        this.input = app.getInputManager();
        this.cam = app.getCamera();
        this.root = app.getRootNode();

        this.game = getStateManager().getState(GameSceneState.class);

        if (!input.hasMapping("Pick")) {
            input.addMapping("Pick", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        }
        input.addListener(clickListener, "Pick");
    }

    @Override protected void cleanup(Application app) {
        if (selectionRing != null) selectionRing.removeFromParent();
        if (input != null) {
            if (input.hasMapping("Pick")) input.deleteMapping("Pick");
            input.removeListener(clickListener);
        }
    }

    @Override protected void onEnable() {}
    @Override protected void onDisable() {}

    private void pickAtCursor() {
        Vector2f cursor = input.getCursorPosition().clone();
        Vector3f from = cam.getWorldCoordinates(cursor, 0f);
        Vector3f to   = cam.getWorldCoordinates(cursor, 1f);
        Ray ray = new Ray(from, to.subtract(from).normalizeLocal());

        CollisionResults results = new CollisionResults();
        root.collideWith(ray, results);
        if (results.size() < 1) { clearSelection(); return; }


        for (CollisionResult cr : results) {
            Spatial g = cr.getGeometry();
            Spatial pieceRoot = ascendToPieceRoot(g);
            if (pieceRoot != null) { selectPiece(pieceRoot); return; }
        }
        clearSelection();
    }

    private Spatial ascendToPieceRoot(Spatial s) {
        Spatial cur = s;
        while (cur != null) {
            Integer flag = cur.getUserData("isPiece");
            if (flag != null && flag == 1) return cur;
            cur = cur.getParent();
        }
        return null;
    }

    private void selectPiece(Spatial piece) {
        this.selectedPiece = piece;

        Integer f = piece.getUserData("file");
        Integer r = piece.getUserData("rank");
        Vector3f center = (f != null && r != null)
                ? game.boardToWorld(f, r)
                : game.boardToWorld(game.worldToBoard(piece.getWorldTranslation())[0],
                                    game.worldToBoard(piece.getWorldTranslation())[1]);

        ensureSelectionRing();
        float yLift = 0.02f; // чуть над доской
        selectionRing.setLocalTranslation(center.add(0, yLift, 0));
    }

    private void clearSelection() {
        this.selectedPiece = null;
        if (selectionRing != null) selectionRing.removeFromParent();
        selectionRing = null;
    }

    private void ensureSelectionRing() {
        if (selectionRing != null) {
            if (selectionRing.getParent() == null) root.attachChild(selectionRing);
            return;
        }
        float cell = game.getPitch();
        float majorRadius = cell * 0.45f;
        float tubeRadius  = cell * 0.05f;

        Torus mesh = new Torus(32, 16, tubeRadius, majorRadius);
        selectionRing = new Geometry("SelectionRing", mesh);

        Material m = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        m.setColor("Color", new ColorRGBA(1f, 0.9f, 0.25f, 0.9f));
        m.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
        selectionRing.setMaterial(m);
        selectionRing.setQueueBucket(Bucket.Transparent);

        selectionRing.rotate(FastMath.HALF_PI, 0, 0); // положим на XZ
        root.attachChild(selectionRing);
    }
    
    public com.jme3.scene.Spatial getSelectedPiece() {
        return selectedPiece;
    }

}
