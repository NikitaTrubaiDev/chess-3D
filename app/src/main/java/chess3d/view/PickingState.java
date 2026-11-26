package chess3d.view;

import chess3d.model.ChessGame;
import chess3d.model.ChessGame.Move;
import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.font.BitmapText;
import com.jme3.collision.CollisionResults;
import com.jme3.math.Ray;
import com.jme3.scene.Spatial;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Torus;
import java.util.List;

/** Mouse interaction and the small in-game status display. */
public class PickingState extends BaseAppState {
    private SimpleApplication app;
    private GameSceneState scene;
    private ChessGame game = new ChessGame();
    private final Node markers = new Node("Move markers");
    private BitmapText title, status, help;
    private int selectedFile = -1, selectedRank = -1;
    private List<Move> moves = List.of();
    private final ActionListener listener = (name, pressed, tpf) -> {
        if (!isEnabled() || pressed) return;
        if (name.equals("chess.pick")) pick();
        else if (name.equals("chess.restart")) { game = new ChessGame(); scene.renderPosition(game); clear(); updateStatus(); }
    };

    @Override protected void initialize(Application application) {
        app = (SimpleApplication) application;
        scene = getStateManager().getState(GameSceneState.class);
        app.getRootNode().attachChild(markers);
        app.getInputManager().addMapping("chess.pick", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        app.getInputManager().addMapping("chess.restart", new KeyTrigger(KeyInput.KEY_R));
        app.getInputManager().addListener(listener, "chess.pick", "chess.restart");
        title = label("CHESS / 3D", 22, new ColorRGBA(.91f,.86f,.72f,1));
        status = label("", 18, ColorRGBA.White);
        help = label("LEFT CLICK  select / move    |    RIGHT DRAG  orbit    |    WHEEL  zoom    |    R  restart", 14, new ColorRGBA(.74f,.78f,.82f,1));
        updateStatus();
    }
    private BitmapText label(String text, float size, ColorRGBA color) {
        BitmapText label = new BitmapText(app.getAssetManager().loadFont("Interface/Fonts/Default.fnt"));
        label.setSize(size); label.setColor(color); label.setText(text); app.getGuiNode().attachChild(label); return label;
    }
    @Override public void update(float tpf) {
        float height = app.getCamera().getHeight();
        title.setLocalTranslation(24, height - 20, 0);
        status.setLocalTranslation(24, height - 50, 0);
        help.setLocalTranslation(24, 27, 0);
    }
    private void updateStatus() {
        String side = game.turn() == ChessGame.Color.WHITE ? "White" : "Black";
        String text = switch(game.status()) {
            case PLAYING -> side + " to move";
            case CHECK -> side + " to move  /  CHECK";
            case CHECKMATE -> "CHECKMATE  /  " + (game.turn() == ChessGame.Color.WHITE ? "Black" : "White") + " wins";
            case STALEMATE -> "DRAW  /  Stalemate";
            case DRAW_FIFTY_MOVE -> "DRAW  /  Fifty-move rule";
            case DRAW_INSUFFICIENT -> "DRAW  /  Insufficient material";
        };
        status.setText(text);
    }
    private void pick() {
        Vector2f cursor = app.getInputManager().getCursorPosition();
        Vector3f from = app.getCamera().getWorldCoordinates(cursor, 0);
        Vector3f direction = app.getCamera().getWorldCoordinates(cursor, 1).subtractLocal(from);
        float boardY = scene.boardToWorld(0, 0).y;
        if (Math.abs(direction.y) < 0.00001f) return;
        float t = (boardY - from.y) / direction.y;
        if (t < 0) return;
        int[] square = null;
        CollisionResults hits = new CollisionResults();
        app.getRootNode().collideWith(new Ray(from, direction.normalize()), hits);
        for (var hit : hits) {
            Spatial node = hit.getGeometry();
            while (node != null && node.getUserData("isPiece") == null) node = node.getParent();
            if (node != null) {
                square = new int[] { node.getUserData("file"), node.getUserData("rank") };
            }
            // The closest surface occludes anything behind it.
            break;
        }
        if (square == null) square = scene.worldToBoard(from.add(direction.mult(t)));
        if (square == null) { clear(); return; }
        int f = square[0], r = square[1];
        if (selectedFile >= 0 && game.move(new Move(selectedFile, selectedRank, f, r))) {
            scene.renderPosition(game); clear(); updateStatus(); return;
        }
        if (f == selectedFile && r == selectedRank) { clear(); return; }
        moves = game.legalMoves(f, r); clear();
        if (game.pieceAt(f,r) == null || game.pieceAt(f,r).color() != game.turn()) return;
        selectedFile=f; selectedRank=r; moves=game.legalMoves(f,r);
        ring(f,r,.43f,new ColorRGBA(.95f,.75f,.30f,1));
        for (Move move : moves) ring(move.toFile(),move.toRank(),.20f,new ColorRGBA(.42f,.86f,.70f,1));
    }
    private void clear() { selectedFile=-1; selectedRank=-1; moves=List.of(); markers.detachAllChildren(); }
    private void ring(int f,int r,float radius,ColorRGBA color) {
        float pitch=scene.getPitch();
        Geometry g=new Geometry("marker",new Torus(32, 8, pitch*.025f, pitch*radius));
        Material material=new Material(app.getAssetManager(),"Common/MatDefs/Misc/Unshaded.j3md");
        material.setColor("Color",color); g.setMaterial(material);
        g.rotate(FastMath.HALF_PI,0,0);
        g.setLocalTranslation(scene.boardToWorld(f,r).add(0,pitch*.025f,0)); markers.attachChild(g);
    }
    @Override protected void cleanup(Application application) {
        markers.removeFromParent(); title.removeFromParent(); status.removeFromParent(); help.removeFromParent();
        app.getInputManager().removeListener(listener);
        app.getInputManager().deleteMapping("chess.pick"); app.getInputManager().deleteMapping("chess.restart");
    }
    @Override protected void onEnable() {}
    @Override protected void onDisable() {}
}
