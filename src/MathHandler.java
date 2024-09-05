
import jsclub.codefest2024.sdk.base.Node;
import jsclub.codefest2024.sdk.model.GameMap;
import java.awt.Rectangle;

public class MathHandler {
    public static double Distance(Node a, Node b){
        return Math.hypot(a.getX() - b.getX(), b.getY() - a.getY());
    }

    public static double Distance(Node a, int x, int y){
        return Math.hypot(a.getX() - x, y - a.getY());
    }

    public static boolean IsInSafeArea(Node a){
        int safeAreaSize = Bot.GetInstance().MapSize - Bot.Instance.gameMap.getDarkAreaSize();

        return ((a.getX() < safeAreaSize / 2) &&
                (a.getY() < safeAreaSize / 2) &&
                (a.getX() > -safeAreaSize / 2) &&
                (a.getY() > -safeAreaSize / 2));
    }
}
