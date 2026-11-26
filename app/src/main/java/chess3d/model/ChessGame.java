package chess3d.model;

import java.util.ArrayList;
import java.util.List;

/** Rules for a local two-player game. Files and ranks run from zero to seven. */
public final class ChessGame {
    public enum Color { WHITE, BLACK; public Color opposite() { return this == WHITE ? BLACK : WHITE; } }
    public enum Type { PAWN, ROOK, KNIGHT, BISHOP, QUEEN, KING }
    public record Piece(Color color, Type type) {}
    public record Move(int fromFile, int fromRank, int toFile, int toRank) {}
    public enum Status { PLAYING, CHECK, CHECKMATE, STALEMATE, DRAW_FIFTY_MOVE, DRAW_INSUFFICIENT }

    private Piece[][] board = new Piece[8][8];
    private Color turn = Color.WHITE;
    private boolean whiteKingMoved, blackKingMoved;
    private boolean whiteARookMoved, whiteHRookMoved, blackARookMoved, blackHRookMoved;
    private int enPassantFile = -1, enPassantRank = -1, halfMoves;

    public ChessGame() {
        Type[] back = {Type.ROOK, Type.KNIGHT, Type.BISHOP, Type.QUEEN, Type.KING, Type.BISHOP, Type.KNIGHT, Type.ROOK};
        for (int f = 0; f < 8; f++) {
            board[f][0] = new Piece(Color.WHITE, back[f]); board[f][1] = new Piece(Color.WHITE, Type.PAWN);
            board[f][6] = new Piece(Color.BLACK, Type.PAWN); board[f][7] = new Piece(Color.BLACK, back[f]);
        }
    }
    private ChessGame(ChessGame other) {
        for (int f = 0; f < 8; f++) board[f] = other.board[f].clone();
        turn = other.turn; whiteKingMoved = other.whiteKingMoved; blackKingMoved = other.blackKingMoved;
        whiteARookMoved = other.whiteARookMoved; whiteHRookMoved = other.whiteHRookMoved;
        blackARookMoved = other.blackARookMoved; blackHRookMoved = other.blackHRookMoved;
        enPassantFile = other.enPassantFile; enPassantRank = other.enPassantRank; halfMoves = other.halfMoves;
    }
    public Piece pieceAt(int f, int r) { return inside(f,r) ? board[f][r] : null; }
    public Color turn() { return turn; }
    public List<Move> legalMoves(int f, int r) {
        List<Move> moves = new ArrayList<>(); Piece p = pieceAt(f,r);
        if (p == null || p.color() != turn) return moves;
        for (int x = 0; x < 8; x++) for (int y = 0; y < 8; y++) {
            Move m = new Move(f,r,x,y);
            if (pseudoLegal(m, p)) { ChessGame copy = new ChessGame(this); copy.apply(m); if (!copy.inCheck(p.color())) moves.add(m); }
        }
        return moves;
    }
    public boolean move(Move move) {
        if (status() == Status.CHECKMATE || status() == Status.STALEMATE || status() == Status.DRAW_FIFTY_MOVE || status() == Status.DRAW_INSUFFICIENT || !legalMoves(move.fromFile(), move.fromRank()).contains(move)) return false;
        apply(move); return true;
    }
    public Status status() {
        boolean check = inCheck(turn), any = false;
        for (int f=0; f<8 && !any; f++) for (int r=0; r<8 && !any; r++) any = !legalMoves(f,r).isEmpty();
        if (!any) return check ? Status.CHECKMATE : Status.STALEMATE;
        if (halfMoves >= 100) return Status.DRAW_FIFTY_MOVE;
        if (insufficient()) return Status.DRAW_INSUFFICIENT;
        return check ? Status.CHECK : Status.PLAYING;
    }
    private boolean insufficient() {
        int minors = 0; boolean bishopsOnly = true; int bishopColor = -1;
        for (int f=0; f<8; f++) for (int r=0; r<8; r++) {
            Piece p=board[f][r]; if (p==null || p.type()==Type.KING) continue;
            if (p.type()!=Type.BISHOP && p.type()!=Type.KNIGHT) return false;
            minors++; if (p.type()==Type.KNIGHT) bishopsOnly=false;
            else if (bishopColor == -1) bishopColor=(f+r)%2; else if (bishopColor!=(f+r)%2) bishopsOnly=false;
        }
        return minors<=1 || bishopsOnly;
    }
    private boolean pseudoLegal(Move m, Piece p) {
        int f=m.fromFile(), r=m.fromRank(), x=m.toFile(), y=m.toRank(), dx=x-f, dy=y-r;
        if (!inside(x,y) || (dx==0 && dy==0)) return false;
        Piece target=board[x][y]; if (target!=null && (target.color()==p.color() || target.type()==Type.KING)) return false;
        switch (p.type()) {
            case PAWN: {
                int dir=p.color()==Color.WHITE ? 1 : -1, start=p.color()==Color.WHITE ? 1 : 6;
                return dx==0 && target==null && (dy==dir || (r==start && dy==2*dir && board[f][r+dir]==null))
                    || Math.abs(dx)==1 && dy==dir && (target!=null || (x==enPassantFile && y==enPassantRank));
            }
            case KNIGHT: return Math.abs(dx)*Math.abs(dy)==2;
            case KING:
                if (Math.max(Math.abs(dx),Math.abs(dy))==1) return true;
                if (dy!=0 || Math.abs(dx)!=2 || f!=4 || r!=(p.color()==Color.WHITE?0:7) || kingMoved(p.color()) || inCheck(p.color())) return false;
                int rookFile=dx>0?7:0; Piece rook=board[rookFile][r];
                if (rook==null || rook.color()!=p.color() || rook.type()!=Type.ROOK || rookMoved(p.color(),rookFile)) return false;
                for (int i=f+Integer.signum(dx); i!=rookFile; i+=Integer.signum(dx)) if (board[i][r]!=null) return false;
                return !attacked(f+Integer.signum(dx),r,p.color().opposite()) && !attacked(x,r,p.color().opposite());
            case BISHOP: if (Math.abs(dx)!=Math.abs(dy)) return false; break;
            case ROOK: if (dx!=0 && dy!=0) return false; break;
            case QUEEN: if (dx!=0 && dy!=0 && Math.abs(dx)!=Math.abs(dy)) return false; break;
        }
        return clearPath(f,r,x,y);
    }
    private boolean clearPath(int f,int r,int x,int y) {
        int sx=Integer.signum(x-f), sy=Integer.signum(y-r);
        for (int a=f+sx,b=r+sy; a!=x || b!=y; a+=sx,b+=sy) if (board[a][b]!=null) return false;
        return true;
    }
    private boolean inCheck(Color color) {
        for (int f=0;f<8;f++) for(int r=0;r<8;r++) { Piece p=board[f][r]; if(p!=null && p.color()==color && p.type()==Type.KING) return attacked(f,r,color.opposite()); }
        return true;
    }
    private boolean attacked(int x,int y,Color by) {
        for(int f=0;f<8;f++) for(int r=0;r<8;r++) {
            Piece p=board[f][r]; if(p==null || p.color()!=by) continue;
            int dx=x-f,dy=y-r;
            switch(p.type()) {
                case PAWN: if(Math.abs(dx)==1 && dy==(by==Color.WHITE?1:-1)) return true; break;
                case KNIGHT: if(Math.abs(dx)*Math.abs(dy)==2) return true; break;
                case KING: if(Math.max(Math.abs(dx),Math.abs(dy))==1) return true; break;
                case ROOK: if((dx==0 || dy==0) && clearPath(f,r,x,y)) return true; break;
                case BISHOP: if(Math.abs(dx)==Math.abs(dy) && clearPath(f,r,x,y)) return true; break;
                case QUEEN: if((dx==0 || dy==0 || Math.abs(dx)==Math.abs(dy)) && clearPath(f,r,x,y)) return true; break;
            }
        }
        return false;
    }
    private void apply(Move m) {
        int f=m.fromFile(), r=m.fromRank(), x=m.toFile(), y=m.toRank(); Piece p=board[f][r], captured=board[x][y];
        if (captured!=null && captured.type()==Type.ROOK) markRook(captured.color(),x,y);
        if (p.type()==Type.ROOK) markRook(p.color(),f,r);
        if (p.type()==Type.KING) {
            if(p.color()==Color.WHITE) whiteKingMoved=true; else blackKingMoved=true;
            if(Math.abs(x-f)==2) { int rook=x>f?7:0, dest=x>f?5:3; board[dest][r]=board[rook][r]; board[rook][r]=null; }
        }
        if(p.type()==Type.PAWN && x!=f && captured==null) board[x][r]=null;
        board[x][y]=p.type()==Type.PAWN && (y==0 || y==7) ? new Piece(p.color(),Type.QUEEN) : p; board[f][r]=null;
        enPassantFile=-1; enPassantRank=-1;
        if(p.type()==Type.PAWN && Math.abs(y-r)==2) { enPassantFile=f; enPassantRank=(r+y)/2; }
        halfMoves=p.type()==Type.PAWN || captured!=null ? 0 : halfMoves+1;
        turn=turn.opposite();
    }
    private void markRook(Color c,int f,int r) { if(r!=(c==Color.WHITE?0:7)) return; if(c==Color.WHITE) { if(f==0) whiteARookMoved=true; if(f==7) whiteHRookMoved=true; } else { if(f==0) blackARookMoved=true; if(f==7) blackHRookMoved=true; } }
    private boolean kingMoved(Color c) { return c==Color.WHITE?whiteKingMoved:blackKingMoved; }
    private boolean rookMoved(Color c,int f) { return c==Color.WHITE?(f==0?whiteARookMoved:whiteHRookMoved):(f==0?blackARookMoved:blackHRookMoved); }
    private static boolean inside(int f,int r) { return f>=0 && f<8 && r>=0 && r<8; }
}
