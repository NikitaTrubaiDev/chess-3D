package chess3d;

import chess3d.model.ChessGame;
import chess3d.model.ChessGame.Move;
import chess3d.model.ChessGame.Type;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AppTest {
    private static void play(ChessGame game, String from, String to) {
        assertTrue(game.move(new Move(from.charAt(0)-'a', from.charAt(1)-'1', to.charAt(0)-'a', to.charAt(1)-'1')), from + "-" + to);
    }
    @Test void openingAndTurns() {
        ChessGame game = new ChessGame();
        assertEquals(2, game.legalMoves(4,1).size());
        assertFalse(game.move(new Move(4,6,4,4)));
        play(game,"e2","e4"); play(game,"e7","e5");
        assertFalse(game.move(new Move(0,0,0,2)));
    }
    @Test void foolsMate() {
        ChessGame game = new ChessGame();
        play(game,"f2","f3"); play(game,"e7","e5"); play(game,"g2","g4"); play(game,"d8","h4");
        assertEquals(ChessGame.Status.CHECKMATE, game.status());
        assertFalse(game.move(new Move(0,1,0,2)));
    }
    @Test void castlingMovesRookAndKing() {
        ChessGame game = new ChessGame();
        play(game,"e2","e4"); play(game,"a7","a6"); play(game,"g1","f3"); play(game,"a6","a5");
        play(game,"f1","e2"); play(game,"b7","b6"); play(game,"e1","g1");
        assertEquals(Type.KING, game.pieceAt(6,0).type()); assertEquals(Type.ROOK, game.pieceAt(5,0).type());
        assertNull(game.pieceAt(7,0));
    }
    @Test void enPassantRemovesPassedPawn() {
        ChessGame game = new ChessGame();
        play(game,"e2","e4"); play(game,"a7","a6"); play(game,"e4","e5"); play(game,"d7","d5"); play(game,"e5","d6");
        assertNull(game.pieceAt(3,4)); assertEquals(Type.PAWN,game.pieceAt(3,5).type());
    }
}
