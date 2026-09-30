import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import javafx.animation.Animation;
import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;

// main game class with the ui and game loop
public class Game extends Application {

    public static final int WIDTH = 960;
    public static final int HEIGHT = 600;

    private enum MatchMode {
        TWO_PLAYER("2-Player"),
        EASY_AI("Easy AI"),
        HARD_AI("Hard AI");

        private final String label;


        MatchMode(String label) {
            this.label = label;
        }


        public String getLabel() {
            return label;
        }
    }

    private static final int WIN_SCORE = 5;
    private static final String TITLE = "Football Duel";
    private static final String AUTHORS = "Arjun Dhir, Rithik Janarthanan, Reon Carroll-Ito, Shael Kumar";

    private final Pitch pitch = new Pitch(70, 50, 820, 500, 32, 190);
    private final Player playerOne = new Player("Player 1", "1",
        Color.rgb(240, 99, 69), Color.WHITE, pitch.getLeftKickoffX(), pitch.getCenterY(), new Vector2(1, 0));
    private final Player playerTwo = new Player("Player 2", "2",
        Color.rgb(52, 152, 219), Color.WHITE, pitch.getRightKickoffX(), pitch.getCenterY(), new Vector2(-1, 0));
    private final Ball ball = new Ball(pitch.getCenterX(), pitch.getCenterY());
    private final ArrayList<GameObject> gameObjects = new ArrayList<>();
    private final Set<KeyCode> activeKeys = new HashSet<>();

    private GraphicsContext graphicsContext;
    private Label playerOneNameLabel;
    private Label playerTwoNameLabel;
    private Label playerOneScoreLabel;
    private Label playerTwoScoreLabel;
    private Label hudTitleLabel;
    private Label hudSubtitleLabel;
    private Label hudModeLabel;
    private Label controlsLabel;
    private Label announcementLabel;
    private Animation announcementAnimation;
    private AnimationTimer animationTimer;
    private MatchMode matchMode = MatchMode.TWO_PLAYER;
    private boolean matchOver;
    private double kickoffPause;
    private double aiDecisionTimer;
    private static final double MAX_FRAME_DELTA     = 0.033;
    private static final double KICKOFF_PAUSE_GOAL  = 1.5;
    private static final double KICKOFF_PAUSE_START = 1.0;
    private static final double AI_DEAD_ZONE_EASY   = 16.0;
    private static final double AI_DEAD_ZONE_HARD   = 10.0;
    private static final double AI_INTERVAL_EASY    = 0.24;
    private static final double AI_INTERVAL_HARD    = 0.07;
    private long previousFrame = -1L;
    private Vector2 aiTarget = new Vector2();
    private Vector2 aiKickDirection = new Vector2(-1, 0);


    public Game() {
        gameObjects.add(playerOne);
        gameObjects.add(playerTwo);
        gameObjects.add(ball);
    }


    public static void main(String[] args) {
        launch(args);
    }


    @Override
    public void start(Stage stage) {
        Canvas canvas = new Canvas(WIDTH, HEIGHT);
        graphicsContext = canvas.getGraphicsContext2D();

        hudTitleLabel = new Label(TITLE);
        hudTitleLabel.setTextFill(Color.YELLOW);
        hudTitleLabel.setFont(Font.font("Verdana", FontWeight.BOLD, 32));
        hudTitleLabel.setAlignment(Pos.CENTER);

        hudSubtitleLabel = new Label("First to score 5 goals wins");
        hudSubtitleLabel.setTextFill(Color.YELLOW);
        hudSubtitleLabel.setFont(Font.font("Verdana", 18));
        hudSubtitleLabel.setAlignment(Pos.CENTER);
        
        hudModeLabel = new Label("Mode: 2-Player");
        hudModeLabel.setTextFill(Color.YELLOW);
        hudModeLabel.setFont(Font.font("Verdana", 18));
        hudModeLabel.setAlignment(Pos.CENTER);

        playerOneNameLabel = new Label("Player 1");
        playerOneScoreLabel = new Label("0");
        playerOneNameLabel.setTextFill(Color.RED);
        playerOneNameLabel.setFont(Font.font("Verdana", FontWeight.BOLD, 26));
        playerOneScoreLabel.setTextFill(Color.RED);
        playerOneScoreLabel.setFont(Font.font("Verdana", FontWeight.BOLD, 52));
        
        playerTwoNameLabel = new Label("Player 2");
        playerTwoScoreLabel = new Label("0");
        playerTwoNameLabel.setTextFill(Color.CYAN);
        playerTwoNameLabel.setFont(Font.font("Verdana", FontWeight.BOLD, 26));
        playerTwoScoreLabel.setTextFill(Color.CYAN);
        playerTwoScoreLabel.setFont(Font.font("Verdana", FontWeight.BOLD, 52));
        
        VBox playerOneBox = new VBox(2, playerOneNameLabel, playerOneScoreLabel);
        playerOneBox.setAlignment(Pos.CENTER_LEFT);
        
        VBox playerTwoBox = new VBox(2, playerTwoNameLabel, playerTwoScoreLabel);
        playerTwoBox.setAlignment(Pos.CENTER_RIGHT);

        VBox hudCenter = new VBox(2, hudTitleLabel, hudSubtitleLabel, hudModeLabel);
        hudCenter.setAlignment(Pos.CENTER);
        
        BorderPane hudRow = new BorderPane();
        hudRow.setLeft(playerOneBox);
        hudRow.setCenter(hudCenter);
        hudRow.setRight(playerTwoBox);
        hudRow.setPadding(new Insets(10, 14, 10, 14));
        hudRow.setStyle("-fx-background-color: #003eaa;");
        
        BorderPane.setAlignment(playerOneScoreLabel, Pos.CENTER_LEFT);
        BorderPane.setAlignment(playerTwoScoreLabel, Pos.CENTER_RIGHT);

        controlsLabel = new Label();
        controlsLabel.setTextFill(Color.WHITE);
        controlsLabel.setFont(Font.font("Verdana", 14));
        controlsLabel.setPadding(new Insets(10));
        controlsLabel.setAlignment(Pos.CENTER);
        controlsLabel.setMaxWidth(Double.MAX_VALUE);
        controlsLabel.setStyle("-fx-background-color: #008833;");

        announcementLabel = new Label();
        announcementLabel.setFont(Font.font("Verdana", FontWeight.EXTRA_BOLD, 38));
        announcementLabel.setTextFill(Color.WHITE);
        announcementLabel.setVisible(false);
        announcementLabel.setMouseTransparent(true);
        announcementLabel.setAlignment(Pos.CENTER);
        announcementLabel.setStyle(
            "-fx-background-color: rgba(0, 51, 170, 0.84);" +
            "-fx-padding: 18 26 18 26;" +
            "-fx-background-radius: 16;");

        StackPane centerPane = new StackPane(canvas, announcementLabel);
        centerPane.setPadding(new Insets(10));
        centerPane.setStyle("-fx-background-color: #66aaff;");
        centerPane.setOnMouseClicked(event -> centerPane.requestFocus());

        MenuBar menuBar = createMenuBar();

        VBox topBox = new VBox(menuBar, hudRow);
        BorderPane root = new BorderPane();
        root.setTop(topBox);
        root.setCenter(centerPane);
        root.setBottom(controlsLabel);
        root.setStyle("-fx-background-color: #66aaff;");
        root.setFocusTraversable(true);

        Scene scene = new Scene(root, WIDTH + 24, HEIGHT + 190);
        scene.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                handleKeyPressed(event);
            }
        });
        scene.setOnKeyReleased(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                handleKeyReleased(event);
            }
        });

        stage.setTitle(TITLE);
        stage.setResizable(true);
        stage.setMinWidth(WIDTH + 24);
        stage.setMinHeight(HEIGHT + 190);
        stage.setScene(scene);
        stage.show();

        restartMatch();
        root.requestFocus();

        animationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (previousFrame < 0) {
                    previousFrame = now;
                    draw();
                    return;
                }

                // Caps the frame step otherwise it goes haywire
                double dt = Math.min((now - previousFrame) / 1000000000.0, MAX_FRAME_DELTA);
                previousFrame = now;

                update(dt);
                draw();
            }
        };

        animationTimer.start();
    }


    private MenuBar createMenuBar() {
        MenuItem restartItem = new MenuItem("Restart Match");
        restartItem.setOnAction(event -> restartMatch());

        MenuItem quitItem = new MenuItem("Quit");
        quitItem.setOnAction(event -> Platform.exit());

        Menu fileMenu = new Menu("File");
        fileMenu.getItems().add(restartItem);
        fileMenu.getItems().add(quitItem);

        MenuItem twoPlayerItem = new MenuItem("2 Player");
        twoPlayerItem.setOnAction(event -> setMatchMode(MatchMode.TWO_PLAYER));

        MenuItem easyAiItem = new MenuItem("Easy AI");
        easyAiItem.setOnAction(event -> setMatchMode(MatchMode.EASY_AI));

        MenuItem hardAiItem = new MenuItem("Hard AI");
        hardAiItem.setOnAction(event -> setMatchMode(MatchMode.HARD_AI));

        Menu modeMenu = new Menu("Mode");
        modeMenu.getItems().add(twoPlayerItem);
        modeMenu.getItems().add(easyAiItem);
        modeMenu.getItems().add(hardAiItem);

        MenuItem controlsItem = new MenuItem("Controls");
        controlsItem.setOnAction(event -> showControlsDialog());

        MenuItem aboutItem = new MenuItem("About");
        aboutItem.setOnAction(event -> showAboutDialog());

        Menu helpMenu = new Menu("Help");
        helpMenu.getItems().add(controlsItem);
        helpMenu.getItems().add(aboutItem);

        MenuBar menuBar = new MenuBar();
        menuBar.getMenus().add(fileMenu);
        menuBar.getMenus().add(modeMenu);
        menuBar.getMenus().add(helpMenu);
        return menuBar;
    }


    private void showControlsDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Controls");
        alert.setHeaderText(TITLE + " Controls");
        alert.setContentText(
            "Player 1 moves with W A S D and kicks with Space.\n" +
            "In 2 Player mode Player 2 moves with the arrow keys and kicks with Enter.\n" +
            "Use the Mode menu to switch between Easy AI or Hard AI.\n\n" +
            "First player to score 5 goals wins the match.");
        alert.showAndWait();
    }


    private void showAboutDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About");
        alert.setHeaderText(TITLE);
        alert.setContentText(
            "Authors: " + AUTHORS + "\n\n" +
            "A football game built with JavaFX vector graphics.\n" +
            "Play in 2-Player mode or against Easy AI and Hard AI.\n" +
            "Use the menu to restart or quit the match.");
        alert.showAndWait();
    }


    private void handleKeyPressed(KeyEvent event) {
        KeyCode code = event.getCode();
        activeKeys.add(code);

        if (code == KeyCode.SPACE && !matchOver && kickoffPause <= 0) {
            playerOne.tryKick(ball);
        }

        if (code == KeyCode.ENTER && !isAiMode() && !matchOver && kickoffPause <= 0) {
            playerTwo.tryKick(ball);
        }

        if (code == KeyCode.R) {
            restartMatch();
        }
    }


    private void handleKeyReleased(KeyEvent event) {
        activeKeys.remove(event.getCode());
    }


    private void update(double dt) {
        syncInput(dt);

        if (matchOver) {
            playerOne.setInput(false, false, false, false);
            playerTwo.setInput(false, false, false, false);
            return;
        }

        if (kickoffPause > 0) {
            // gives players a short reset after each goal
            kickoffPause -= dt;
            return;
        }

        for (GameObject object : gameObjects) {
            object.update(dt);
        }

        CollisionMath.resolveCircleCollision(playerOne, playerTwo, 0.82);
        CollisionMath.resolveCircleCollision(playerOne, ball, 0.08);
        CollisionMath.resolveCircleCollision(playerTwo, ball, 0.08);

        pitch.confinePlayer(playerOne);
        pitch.confinePlayer(playerTwo);
        pitch.bounceBall(ball);

        int goal = pitch.checkGoal(ball);
        if (goal != Pitch.NO_GOAL) {
            awardGoal(goal);
        }
    }


    private void syncInput(double dt) {
        playerOne.setInput(
            activeKeys.contains(KeyCode.W),
            activeKeys.contains(KeyCode.S),
            activeKeys.contains(KeyCode.A),
            activeKeys.contains(KeyCode.D));

        if (isAiMode()) {
            syncAiInput(dt);
        }
        else {
            playerTwo.setInput(
                activeKeys.contains(KeyCode.UP),
                activeKeys.contains(KeyCode.DOWN),
                activeKeys.contains(KeyCode.LEFT),
                activeKeys.contains(KeyCode.RIGHT));
        }
    }


    private void syncAiInput(double dt) {
        aiDecisionTimer -= dt;

        if (aiDecisionTimer <= 0) {
            aiDecisionTimer = (matchMode == MatchMode.EASY_AI) ? AI_INTERVAL_EASY : AI_INTERVAL_HARD;
            aiKickDirection = chooseAiKickDirection();
            aiTarget = chooseAiTarget(aiKickDirection);
        }

        double deadZone = (matchMode == MatchMode.EASY_AI) ? AI_DEAD_ZONE_EASY : AI_DEAD_ZONE_HARD;
        playerTwo.setInput(
            playerTwo.getY() > (aiTarget.getY() + deadZone),
            playerTwo.getY() < (aiTarget.getY() - deadZone),
            playerTwo.getX() > (aiTarget.getX() + deadZone),
            playerTwo.getX() < (aiTarget.getX() - deadZone));

        tryAiKick();
    }


    private Vector2 chooseAiTarget(Vector2 kickDirection) {
        if (matchMode == MatchMode.EASY_AI) {
            return chooseEasyAiTarget(kickDirection);
        }

        return chooseHardAiTarget(kickDirection);
    }


    private Vector2 chooseEasyAiTarget(Vector2 kickDirection) {
        double centerX = pitch.getCenterX();
        double homeX = pitch.getRightKickoffX();
        double homeY = pitch.getCenterY();
        double dangerLine = centerX + 35;
        Vector2 clearDirection = chooseWallClearDirection();

        if (clearDirection != null) {
            return chooseWallRecoveryTarget(clearDirection, 34, 54);
        }

        if (isBallThreateningAiGoal()) {
            return chooseGoalSideIntercept(28, 0.18);
        }

        // easy ai waits deeper unless the ball comes into its half
        if (ball.getX() < dangerLine && ball.getVelocity().getX() < 40) {
            return clampAiTarget(new Vector2(homeX, homeY + ((ball.getY() - homeY) * 0.35)));
        }

        Vector2 target = chooseAttackPosition(kickDirection, 26);
        return clampAiTarget(target);
    }


    private Vector2 chooseHardAiTarget(Vector2 kickDirection) {
        double predictedX = ball.getX() + (ball.getVelocity().getX() * 0.22);
        double predictedY = ball.getY() + (ball.getVelocity().getY() * 0.22);
        Vector2 clearDirection = chooseWallClearDirection();

        if (clearDirection != null) {
            return chooseWallRecoveryTarget(clearDirection, 46, 68);
        }

        if (isBallThreateningAiGoal()) {
            return chooseGoalSideIntercept(42, 0.24);
        }

        if (predictedX > (pitch.getCenterX() + 40) || ball.getVelocity().getX() > 70) {
            // hard ai meets the ball earlier when it is threatening its goal
            Vector2 target = chooseAttackPosition(kickDirection, 18);
            target.set(Math.max(target.getX(), predictedX - 10), predictedY);
            return clampAiTarget(target);
        }

        return clampAiTarget(chooseAttackPosition(kickDirection, 18));
    }


    private Vector2 chooseAttackPosition(Vector2 kickDirection, double sideOffset) {
        Vector2 target = ball.getPosition().copy().subtract(kickDirection.copy().scale(sideOffset));
        Vector2 sideStep = new Vector2(-kickDirection.getY(), kickDirection.getX()).scale(10);

        // A sideways offset helps the ai avoid pinning the ball straight into walls
        if (ball.getY() < (pitch.getTop() + 70)) {
            target.add(sideStep);
        }
        else if (ball.getY() > (pitch.getBottom() - 70)) {
            target.subtract(sideStep);
        }

        return target;
    }


    private Vector2 chooseAiKickDirection() {
        if (matchMode == MatchMode.EASY_AI) {
            return chooseEasyAiKickDirection();
        }

        return chooseHardAiKickDirection();
    }


    private Vector2 chooseEasyAiKickDirection() {
        Vector2 clearDirection = chooseWallClearDirection();
        if (clearDirection != null) {
            return clearDirection;
        }

        return new Vector2(-1, (pitch.getCenterY() - ball.getY()) * 0.012).normalize();
    }


    private Vector2 chooseHardAiKickDirection() {
        Vector2 clearDirection = chooseWallClearDirection();
        if (clearDirection != null) {
            return clearDirection;
        }

        double leftGoalX = pitch.getLeft() - 24;
        double leftGoalY = pitch.getCenterY();
        double leadY = ball.getY() + (ball.getVelocity().getY() * 0.22);
        return new Vector2(leftGoalX - ball.getX(), leftGoalY - leadY).normalize();
    }


    private Vector2 chooseWallClearDirection() {
        double rightWall = pitch.getRight() - (ball.getRadius() + 12);
        double topWall = pitch.getTop() + (ball.getRadius() + 18);
        double bottomWall = pitch.getBottom() - (ball.getRadius() + 18);
        boolean nearRightWall = ball.getX() >= rightWall;
        boolean nearTopWall = ball.getY() <= topWall;
        boolean nearBottomWall = ball.getY() >= bottomWall;

        // At the right wall the ai can bank it off the wall to pop it free.
        if (nearRightWall && nearTopWall) {
            return new Vector2(1, 0.90).normalize();
        }

        if (nearRightWall && nearBottomWall) {
            return new Vector2(1, -0.90).normalize();
        }

        if (nearRightWall) {
            double bankY = (pitch.getCenterY() - ball.getY()) * 0.020;
            if (Math.abs(bankY) < 0.28) {
                bankY = (ball.getY() < pitch.getCenterY()) ? 0.28 : -0.28;
            }

            return new Vector2(1, bankY).normalize();
        }

        if (nearTopWall) {
            return new Vector2(-0.72, -1).normalize();
        }

        if (nearBottomWall) {
            return new Vector2(-0.72, 1).normalize();
        }

        return null;
    }


    private Vector2 chooseWallRecoveryTarget(Vector2 kickDirection, double verticalOffset,
                                             double horizontalOffset) {
        double targetX = ball.getX() - horizontalOffset;
        double targetY;
        double topZone = pitch.getTop() + 72;
        double bottomZone = pitch.getBottom() - 72;

        if (kickDirection.getX() > 0) {
            // for bank shots the ai needs to sit inside the pitch and off to one side
            if (kickDirection.getY() > 0) {
                targetY = ball.getY() + verticalOffset;
            }
            else {
                targetY = ball.getY() - verticalOffset;
            }

            return clampAiTarget(new Vector2(targetX, targetY));
        }

        // pull away from the wall first so the bot can come back in on an angle
        if (ball.getY() <= topZone) {
            targetY = ball.getY() + verticalOffset;
        }
        else if (ball.getY() >= bottomZone) {
            targetY = ball.getY() - verticalOffset;
        }
        else if (Math.abs(playerTwo.getY() - ball.getY()) < 24) {
            if (playerTwo.getY() <= ball.getY()) {
                targetY = ball.getY() + verticalOffset;
            }
            else {
                targetY = ball.getY() - verticalOffset;
            }
        }
        else {
            targetY = ball.getY();
        }

        return clampAiTarget(new Vector2(targetX, targetY));
    }


    private boolean isBallThreateningAiGoal() {
        return ball.getX() > (pitch.getCenterX() + 20)
            && ball.getVelocity().getX() > 35;
    }


    private Vector2 chooseGoalSideIntercept(double sideOffset, double lookAhead) {
        double predictedX = ball.getX() + (ball.getVelocity().getX() * lookAhead);
        double predictedY = ball.getY() + (ball.getVelocity().getY() * lookAhead);
        double targetX = Math.max(ball.getX() + sideOffset, predictedX + sideOffset);
        double targetY = predictedY;

        // a small vertical lean helps the bot swing around the ball instead of escorting it
        if (ball.getY() < pitch.getCenterY()) {
            targetY += 16;
        }
        else {
            targetY -= 16;
        }

        return clampAiTarget(new Vector2(targetX, targetY));
    }


    private Vector2 clampAiTarget(Vector2 target) {
        double minX = pitch.getLeft() + playerTwo.getRadius();
        double maxX = pitch.getRight() - playerTwo.getRadius();
        double minY = pitch.getTop() + playerTwo.getRadius();
        double maxY = pitch.getBottom() - playerTwo.getRadius();

        return new Vector2(
            Math.max(minX, Math.min(maxX, target.getX())),
            Math.max(minY, Math.min(maxY, target.getY())));
    }


    private void tryAiKick() {
        if (matchOver || kickoffPause > 0) {
            return;
        }

        Vector2 toBall = ball.getPosition().copy().subtract(playerTwo.getPosition());
        Vector2 clearDirection = chooseWallClearDirection();
        double contactRange = playerTwo.getRadius() + ball.getRadius() + 8;
        double maxKickY = (matchMode == MatchMode.EASY_AI) ? 24 : 34;
        double maxKickX = (matchMode == MatchMode.EASY_AI) ? 18 : 26;

        if (clearDirection != null) {
            double verticalGap = playerTwo.getY() - ball.getY();

            if (clearDirection.getX() > 0) {
                // wait until the bot is on the right side of the bank shot
                if ((clearDirection.getY() > 0 && verticalGap < 12)
                    || (clearDirection.getY() < 0 && verticalGap > -12)) {
                    return;
                }
            }
            else if (Math.abs(clearDirection.getY()) > Math.abs(clearDirection.getX())) {
                // for top and bottom walls the ai needs to hit from above or below
                if ((clearDirection.getY() < 0 && verticalGap < 12)
                    || (clearDirection.getY() > 0 && verticalGap > -12)) {
                    return;
                }
            }
            else if (Math.abs(verticalGap) < 18) {
                return;
            }
        }

        // the ai only kicks once it is genuinely on top of the ball
        if (toBall.lengthSquared() <= (contactRange * contactRange)
            && toBall.getX() <= maxKickX
            && Math.abs(toBall.getY()) <= maxKickY) {
            playerTwo.tryKickToward(ball, aiKickDirection);
        }
    }


    private boolean isAiMode() {
        return matchMode != MatchMode.TWO_PLAYER;
    }


    private void setMatchMode(MatchMode newMode) {
        matchMode = newMode;
        aiDecisionTimer = 0;
        updateControlsLabel();
        restartMatch();
    }


    private void awardGoal(int goalSide) {
        Player scorer = (goalSide == Pitch.LEFT_GOAL) ? playerTwo : playerOne;

        scorer.incrementScore();
        updateHud();

        if (scorer.getScore() >= WIN_SCORE) {
            matchOver = true;
            kickoffPause = 0;
            playerOne.stop();
            playerTwo.stop();
            ball.stop();
            showWinnerAnnouncement(scorer);
            return;
        }

        resetPositions();
        kickoffPause = KICKOFF_PAUSE_GOAL;
        showGoalAnnouncement(scorer);
    }


    private void restartMatch() {
        playerOne.resetScore();
        playerTwo.resetScore();
        matchOver = false;
        kickoffPause = KICKOFF_PAUSE_START;
        aiDecisionTimer = 0;
        activeKeys.clear();
        resetPositions();
        hideAnnouncement();
        updateControlsLabel();
        updateHud();
        previousFrame = -1L;
    }


    private void resetPositions() {
        Vector2 FACING_RIGHT = new Vector2(1, 0);
        Vector2 FACING_LEFT = new Vector2(-1, 0);
        
        playerOne.reset(pitch.getLeftKickoffX(), pitch.getCenterY(), FACING_RIGHT);
        playerTwo.reset(pitch.getRightKickoffX(), pitch.getCenterY(), FACING_LEFT);
        ball.reset(pitch.getCenterX(), pitch.getCenterY());
    }


    private void updateHud() {
        // todo if someone redesigns the hud this is the main text to change
        playerOneScoreLabel.setText(String.valueOf(playerOne.getScore()));
        playerTwoScoreLabel.setText(String.valueOf(playerTwo.getScore()));
        
        hudSubtitleLabel.setText(matchOver ? "Match complete" : "First to score 5 goals wins");
        hudModeLabel.setText(matchOver ? "Match complete" : "Mode: " + matchMode.getLabel());
    }


    private void updateControlsLabel() {
        if (isAiMode()) {
            controlsLabel.setText(
                "Player 1: WASD + Space    Opponent: " + matchMode.getLabel() +
                "    Mode menu changes opponent    R: Restart Match");
        }
        else {
            controlsLabel.setText(
                "Player 1: WASD + Space    Player 2: Arrow Keys + Enter    R: Restart Match");
        }
    }
    
    private void showWinnerAnnouncement(Player winner) {
        playWinnerAnnouncement(
            winner.getName() + " WINS!",
            "Press R or use File -> Restart Match",
            winner.getFillColor(),
            false);
    }
    
    private void showGoalAnnouncement(Player scorer) {
        playGoalAnnouncement(
            scorer.getName() + " SCORES!",
            "Kick-off resumes in a moment",
            Color.GOLD,
            true);
    }

    private void playWinnerAnnouncement(String title, String subtitle, Color color, boolean fadeOut) {
        if (announcementAnimation != null) {
            announcementAnimation.stop();
        }

        announcementLabel.setStyle("-fx-background-color: rgba(255, 215, 0, 0.85);" 
        + "-fx-padding: 18 26 18 26;" + "-fx-background-radius: 16;" + "-fx-border-color: #ffffff;"
        + "-fx-border-width: 3;" + "-fx-border-radius: 16;");
        
        announcementLabel.setText(title + "\n" + subtitle);
        announcementLabel.setTextFill(color);
        announcementLabel.setOpacity(1.0);
        announcementLabel.setScaleX(0.65);
        announcementLabel.setScaleY(0.65);
        announcementLabel.setVisible(true);

        ScaleTransition scale = new ScaleTransition(Duration.seconds(0.45), announcementLabel);
        scale.setToX(1.0);
        scale.setToY(1.0);

        if (fadeOut) {
            FadeTransition fade = new FadeTransition(Duration.seconds(0.45), announcementLabel);
            fade.setFromValue(1.0);
            fade.setToValue(0.0);
            
            ParallelTransition transition = new ParallelTransition(scale, fade);
            transition.setOnFinished(event -> announcementLabel.setVisible(false));
            announcementAnimation = transition;
        }
        else {
            // For the winner announcement, play scale and just stop to keep it on-screen
            announcementAnimation = scale;
        }
        
        announcementAnimation.playFromStart();
    }
    
    private void playGoalAnnouncement(String title, String subtitle, Color color, boolean fadeOut) {
        if (announcementAnimation != null) {
            announcementAnimation.stop();
        }
        
        announcementLabel.setStyle("-fx-background-color: rgba(0, 51, 170, 0.84);" 
        + "-fx-padding: 18 26 18 26;" + "-fx-background-radius: 16;");
        
        announcementLabel.setText(title + "\n" + subtitle);
        announcementLabel.setTextFill(color);
        announcementLabel.setOpacity(1.0);
        announcementLabel.setScaleX(0.65);
        announcementLabel.setScaleY(0.65);
        announcementLabel.setVisible(true);

        ScaleTransition scale = new ScaleTransition(Duration.seconds(0.45), announcementLabel);
        scale.setToX(1.0);
        scale.setToY(1.0);
        
        if (fadeOut) {
            // Quick Scale Up
            ScaleTransition scaleUp = new ScaleTransition(Duration.seconds(0.2), announcementLabel);
            scaleUp.setToX(1.1);
            scaleUp.setToY(1.1);
            // Stay visible
            PauseTransition hold = new PauseTransition(Duration.seconds(1.2));
            // Smoooth Fade Out
            FadeTransition fade = new FadeTransition(Duration.seconds(0.6), announcementLabel);
            fade.setFromValue(1.0);
            fade.setToValue(0.0);
            // Play them sequentially
            SequentialTransition sequence = new SequentialTransition(scaleUp, hold, fade);
            
            sequence.setOnFinished(event -> announcementLabel.setVisible(false));
            announcementAnimation = sequence;            
        }
        
        announcementAnimation.playFromStart();
    }


    private void hideAnnouncement() {
        if (announcementAnimation != null) {
            announcementAnimation.stop();
        }

        announcementLabel.setVisible(false);
        announcementLabel.setOpacity(1.0);
    }


    private void draw() {
        pitch.draw(graphicsContext);

        for (GameObject object : gameObjects) {
            object.draw(graphicsContext);
        }
    }
}
