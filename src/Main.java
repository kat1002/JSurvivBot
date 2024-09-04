import io.socket.emitter.Emitter;
import jsclub.codefest2024.sdk.algorithm.PathUtils;
import jsclub.codefest2024.sdk.base.Node;
import jsclub.codefest2024.sdk.model.GameMap;
import jsclub.codefest2024.sdk.Hero;
import jsclub.codefest2024.sdk.model.obstacles.Obstacle;
import jsclub.codefest2024.sdk.model.players.Player;
import jsclub.codefest2024.sdk.model.weapon.Weapon;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class Main {
    private static final String SERVER_URL = "https://cf-server.jsclub.dev";
    private static final String GAME_ID = "180294";
    private static final String PLAYER_NAME = "test";

    public static void main(String[] args) throws IOException {
        Hero hero = new Hero(GAME_ID, PLAYER_NAME);

        Emitter.Listener onMapUpdate = new Emitter.Listener() {
            @Override
            public void call(Object... args) {

                try {
                    GameMap gameMap = hero.getGameMap();
                    gameMap.updateOnUpdateMap(args[0]);

                    Player player = gameMap.getCurrentPlayer();
                    List<Player> otherPlayers = gameMap.getOtherPlayerInfo();
                    List<Obstacle> restricedList = gameMap.getListIndestructibleObstacles();
                    restricedList.addAll(gameMap.getListChests());
                    restricedList.addAll(gameMap.getListTraps());
                    Node currentNode = new Node(player.getX(), player.getY());
                    List<Node> restrictedNodes = new ArrayList<>();

                    List<Node> otherPlayesNode = new ArrayList<>();

                    for (Player p : otherPlayers) {
                        if(p.getIsAlive()){
                            otherPlayesNode.add(new Node(p.getX(), p.getY()));
                        }
                    }
                    for (Obstacle o : restricedList) {
                        restrictedNodes.add(new Node(o.getX(), o.getY()));
                    }

                    Weapon isUseGun = hero.getInventory().getGun();
                    final boolean[] pickedUpGun = {isUseGun != null};

                    System.out.println("inventory: " + isUseGun);
                    System.out.println("is picked up: " + pickedUpGun[0]);

                    if (!pickedUpGun[0]) {
                        List<Weapon> gunList = gameMap.getAllGun();
                        Weapon someGun = gunList.get(0);

                        if (currentNode.getX() == someGun.getX() && currentNode.getY() == someGun.getY()) {
                            hero.pickupItem();
                            pickedUpGun[0] = true;
                        } else {
                            restrictedNodes.addAll(otherPlayesNode);
                            hero.move(PathUtils.getShortestPath(gameMap, restrictedNodes, currentNode, someGun, false));
                        }
                    }

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };

        hero.setOnMapUpdate(onMapUpdate);
        hero.start(SERVER_URL);
    }
}