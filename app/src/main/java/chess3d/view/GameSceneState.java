package chess3d.view;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.bounding.BoundingBox;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

/**
 * Загружает доску и фигуры, выравнивает по якорям A1/H1/A8 (или по bbox, если якорей нет),
 * расставляет стартовую позицию. Свет и камера живут в отдельных AppStates.
 */
public class GameSceneState extends BaseAppState {

    // Имена файлов в /assets
    private static final String BOARD_MODEL = "board.glb";
    private static final String W_PAWN   = "wpawn.glb";
    private static final String W_ROOK   = "wrook.glb";
    private static final String W_KNIGHT = "wknight.glb";
    private static final String W_BISHOP = "wbishop.glb";
    private static final String W_QUEEN  = "wqueen.glb";
    private static final String W_KING   = "wking.glb";
    private static final String B_PAWN   = "bpawn.glb";
    private static final String B_ROOK   = "brook.glb";
    private static final String B_KNIGHT = "bknight.glb";
    private static final String B_BISHOP = "bbishop.glb";
    private static final String B_QUEEN  = "bqueen.glb";
    private static final String B_KING   = "bking.glb";

    private final Node pivot;                 // пивот орбитальной камеры (из App)

    private SimpleApplication sapp;
    private AssetManager assets;

    private Node sceneNode, boardNode, piecesNode;

    // Плоскость верха доски и шаги сетки
    private float boardTopY = 0f;
    private boolean hasAnchors = false;
    private Vector3f a1, xAxis, yAxis;
    private float pitchX = 1f, pitchY = 1f;   // шаг по файлам и рангам
    private float squareSize = 1f;            // шаг для фолбэка

    private BoundingBox sceneBounds;

    // Прототипы фигур
    private Spatial wp, wr, wn, wb, wq, wk;
    private Spatial bp, br, bn, bb, bq, bk;

    public GameSceneState(Node pivot) {
        this.pivot = pivot;
    }

    @Override
    protected void initialize(Application app) {
        this.sapp = (SimpleApplication) app;
        this.assets = app.getAssetManager();

        var root = sapp.getRootNode();
        sceneNode = new Node("ChessScene");
        root.attachChild(sceneNode);

        // ---------- Доска ----------
        boardNode = new Node("BoardNode");
        Spatial board = tryLoad(BOARD_MODEL);
        if (board != null) {
            boardNode.attachChild(board);
            sceneNode.attachChild(boardNode);

            // центрируем GLB по XZ в (0,0)
            board.updateGeometricState();
            BoundingBox bb = (BoundingBox) board.getWorldBound();
            Vector3f c = bb.getCenter().clone();
            board.move(-c.x, 0f, -c.z);

            // пересчитать bbox и верх
            board.updateGeometricState();
            bb = (BoundingBox) board.getWorldBound();
            boardTopY = bb.getCenter().y + bb.getYExtent();

            // якоря
            Spatial a1Node = findByName(board, "A1");
            Spatial h1Node = findByName(board, "H1");
            Spatial a8Node = findByName(board, "A8");

            if (a1Node != null && h1Node != null && a8Node != null) {
                board.updateGeometricState();

                Vector3f A1 = a1Node.getWorldTranslation().clone();
                Vector3f H1 = h1Node.getWorldTranslation().clone();
                Vector3f A8 = a8Node.getWorldTranslation().clone();

                Vector3f vx = H1.subtract(A1);
                Vector3f vy = A8.subtract(A1);

                // Между центрами A1↔H1 и A1↔A8 — 7 шагов
                float spanX = vx.length();
                float spanY = vy.length();
                if (spanX > 1e-5f && spanY > 1e-5f) {
                    pitchX = spanX / 7f;
                    pitchY = spanY / 7f;

                    xAxis = vx.normalize();
                    yAxis = vy.normalize();
                    a1 = A1;
                    hasAnchors = true;

                    // центр игрового поля для пивота камеры
                    Vector3f center = a1.add(xAxis.mult(pitchX * 3.5f))
                                        .add(yAxis.mult(pitchY * 3.5f));
                    pivot.setLocalTranslation(center.x, bb.getCenter().y, center.z);

                    squareSize = 0.5f * (pitchX + pitchY);
                } else {
                    computeFallbackGrid(bb);
                }
            } else {
                computeFallbackGrid(bb);
            }
        } else {
            sceneNode.attachChild(boardNode);
            pivot.setLocalTranslation(Vector3f.ZERO);
        }

        // ---------- Фигуры ----------
        piecesNode = new Node("Pieces");
        sceneNode.attachChild(piecesNode);

        wp = tryLoad(W_PAWN);   wr = tryLoad(W_ROOK);   wn = tryLoad(W_KNIGHT);
        wb = tryLoad(W_BISHOP); wq = tryLoad(W_QUEEN);  wk = tryLoad(W_KING);
        bp = tryLoad(B_PAWN);   br = tryLoad(B_ROOK);   bn = tryLoad(B_KNIGHT);
        bb = tryLoad(B_BISHOP); bq = tryLoad(B_QUEEN);  bk = tryLoad(B_KING);

        normalizePrototype(wp); normalizePrototype(wr); normalizePrototype(wn);
        normalizePrototype(wb); normalizePrototype(wq); normalizePrototype(wk);
        normalizePrototype(bp); normalizePrototype(br); normalizePrototype(bn);
        normalizePrototype(bb); normalizePrototype(bq); normalizePrototype(bk);

        placeStartPosition();
        // [ADD] проставить userData для уже стоящих фигур
        indexExistingPieces();


        // ---------- Границы всей сцены ----------
        sceneNode.updateGeometricState();
        sceneBounds = (BoundingBox) sceneNode.getWorldBound();
    }

    @Override protected void cleanup(Application app) {
        if (sceneNode != null) sceneNode.removeFromParent();
    }
    @Override protected void onEnable() {}
    @Override protected void onDisable() {}

    public BoundingBox getSceneBounds() { return sceneBounds; }

    // ================== ВСПОМОГАТЕЛЬНЫЕ ==================

    private Spatial tryLoad(String path) {
        try { return assets.loadModel(path); }
        catch (Exception e) { System.out.println("[GameScene] Can't load " + path); return null; }
    }

    // Поиск по имени (без учёта регистра)
    private Spatial findByName(Spatial root, String want) {
        if (root == null) return null;
        String n = root.getName();
        if (n != null && n.trim().equalsIgnoreCase(want)) return root;
        if (root instanceof Node) {
            for (Spatial ch : ((Node) root).getChildren()) {
                Spatial s = findByName(ch, want);
                if (s != null) return s;
            }
        }
        return null;
    }

    // Фолбэк: шаг клетки по пропорциям всей доски (S=55, G=2, F=3)
    private void computeFallbackGrid(BoundingBox bb) {
        float wX = bb.getXExtent() * 2f;
        float wZ = bb.getZExtent() * 2f;
        float w  = Math.min(wX, wZ);
        float r = 2f / 55f; // gap/cell
        float f = 3f / 55f; // frameGap/cell
        float cell  = w / (8f + 7f*r + 2f*f);
        float pitch = cell * (1f + r);
        squareSize = pitch;
        pivot.setLocalTranslation(0f, bb.getCenter().y, 0f);
        hasAnchors = false;
        pitchX = pitchY = pitch;
    }

    // Центр клетки -> мировые координаты
    private Vector3f squareToWorld(int file, int rank) {
        if (hasAnchors) {
            Vector3f base = a1
                .add(xAxis.mult(pitchX * file))
                .add(yAxis.mult(pitchY * rank));
            return new Vector3f(base.x, boardTopY, base.z);
        } else {
            float x = (file - 3.5f) * squareSize;
            float z = (rank - 3.5f) * squareSize;
            return new Vector3f(x, boardTopY, z);
        }
    }

    // Создать клон фигуры и поставить на клетку (опереть на верх доски)
    private void spawn(Spatial proto, int file, int rank) {
        if (proto == null) return;
        Spatial p = proto.clone();

        // Повернуть белого коня на 180° вокруг вертикали (оси Y)
        if (proto == wn) {
            p.rotate(0f, FastMath.PI, 0f);
        }

        Vector3f pos = squareToWorld(file, rank);
        p.setLocalTranslation(pos.x, 0f, pos.z);
        piecesNode.attachChild(p);

        p.updateGeometricState();
        BoundingBox pb = (BoundingBox) p.getWorldBound();
        float minY = pb.getCenter().y - pb.getYExtent();
        p.move(0f, boardTopY - minY, 0f);
    }


    // Сброс локальных трансформов и центр по XZ (если origin уже в центре низа — можно убрать move)
    private void normalizePrototype(Spatial s) {
        if (s == null) return;
        s.setLocalTranslation(0, 0, 0);
        s.setLocalRotation(Quaternion.IDENTITY);
        s.setLocalScale(1f);
        s.updateGeometricState();
        s.updateGeometricState();
    }

    // Стартовая расстановка (белые у букв)
    private void placeStartPosition() {
        // белые
        spawn(wr, 0, 0); spawn(wn, 1, 0); spawn(wb, 2, 0); spawn(wq, 3, 0);
        spawn(wk, 4, 0); spawn(wb, 5, 0); spawn(wn, 6, 0); spawn(wr, 7, 0);
        for (int f = 0; f < 8; f++) spawn(wp, f, 1);

        // чёрные
        spawn(br, 0, 7); spawn(bn, 1, 7); spawn(bb, 2, 7); spawn(bq, 3, 7);
        spawn(bk, 4, 7); spawn(bb, 5, 7); spawn(bn, 6, 7); spawn(br, 7, 7);
        for (int f = 0; f < 8; f++) spawn(bp, f, 6);
    }
        // === [ADD] Преобразования доска↔мир + маркировка фигур ===

    /** Публичный доступ к центру клетки в world-space. */
    public Vector3f boardToWorld(int file, int rank) {
        return squareToWorld(file, rank);
    }

    /** Перевод мировой точки в индексы клетки (к ближайшему центру), с ограничением [0..7]. */
    public int[] worldToBoard(Vector3f worldPoint) {
        int file, rank;
        if (hasAnchors) {
            Vector3f v = worldPoint.subtract(a1);
            float u = v.dot(xAxis) / pitchX; // вдоль файлов A→H
            float w = v.dot(yAxis) / pitchY; // вдоль рангов 1→8
            file = Math.round(u);
            rank = Math.round(w);
        } else {
            float x = worldPoint.x;
            float z = worldPoint.z;
            file = Math.round(x / squareSize + 3.5f);
            rank = Math.round(z / squareSize + 3.5f);
        }
        if (file < 0) file = 0; if (file > 7) file = 7;
        if (rank < 0) rank = 0; if (rank > 7) rank = 7;
        return new int[]{file, rank};
    }

    /** Удобный размер клетки (для визуальных маркеров). */
    public float getPitch() {
        return hasAnchors ? Math.min(pitchX, pitchY) : squareSize;
    }

    /** Пометить фигуру как пикаемую и записать её клетку. */
    public void registerPiece(Spatial piece, int file, int rank) {
        if (piece == null) return;
        piece.setUserData("isPiece", 1);
        piece.setUserData("file", file);
        piece.setUserData("rank", rank);
    }

    /** Проставить userData("isPiece"/"file"/"rank") всем уже стоящим на сцене фигурам. */
    public void indexExistingPieces() {
        if (piecesNode == null) return;
        for (Spatial s : piecesNode.getChildren()) {
            int[] fr = worldToBoard(s.getWorldTranslation());
            registerPiece(s, fr[0], fr[1]);
        }
    }

}
