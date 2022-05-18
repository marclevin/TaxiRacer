package model.display;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

import javafx.animation.Animation;
import javafx.animation.AnimationTimer;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import model.logic.Road;
import model.logic.Settings;
import model.logic.Sprite;
import model.logic.Taxi;
import model.utility.InputImg;

public class GameCanvas extends Canvas {
    private GraphicsContext gc = null;
    private ParallelTransition thisTranslation = null;
    private ArrayList<Sprite> sprites = null;

    public GameCanvas(double width, double height) {
        super(width, height);
        gc = this.getGraphicsContext2D();
        load_sprites();
    };

    public GameCanvas() {
        super(Settings.SCENE_WIDTH, Settings.SCENE_HEIGHT);
        gc = this.getGraphicsContext2D();
        load_sprites();
    }

    public void speed_modify(int x) {
        if (thisTranslation != null) {
            thisTranslation.setRate(x);
        }
    }

    private void load_sprites() {
        Road primary_road = new Road(Settings.PRIMARY_ROAD_X, Settings.ROAD_Y);
        Road road_piece_prime = new Road(Settings.PRIME_ROAD_PIECE_X-150, Settings.ROAD_Y);
        Road road_piece_second = new Road(Settings.SECONDARY_ROAD_PIECE_X+30, Settings.ROAD_Y);
        Road secondary_road = new Road(Settings.SECONDARY_ROAD_X+55, Settings.ROAD_Y);
        Image road_image = load_image(".\\road_try.png");
        Image road_piece_image = load_image(".\\road_piece.png");
        primary_road.setImage(road_image);

        SnapshotParameters param = new SnapshotParameters();
        param.setFill(Color.TRANSPARENT);

        secondary_road.setImage(road_image);
        road_piece_prime.setImage(road_piece_image);
        road_piece_prime.getMyImageView().setFitWidth(700);
        road_piece_second.setImage(road_piece_image);
        // Now for the taxi.
        Taxi taxi = new Taxi(0, Settings.ROAD_Y);
        taxi.setImage(load_image(".\\minibus_modified.png"));
        
        AnimationTimer loop = null;
        InputImg.setImageView(taxi.getMyImageView());
        taxi.getMyImageView().setY(Settings.ROAD_Y+30);
        loop = new AnimationTimer() {
            private int speedAdjust = 5;
            @Override
            public void handle(long now) {
                gc.clearRect(0, 0, 1080, 720);
                gc.setFill(Color.RED);
                gc.fillRect(0, 0, Settings.SCENE_WIDTH, Settings.SCENE_HEIGHT);
                gc.drawImage(primary_road.getMyImageView().snapshot(param, null), primary_road.getX(), primary_road.getY());
                gc.drawImage(road_piece_prime.getMyImageView().snapshot(param, null), road_piece_prime.getX(),
                        road_piece_prime.getY());
                gc.drawImage(secondary_road.getMyImageView().getImage(), secondary_road.getX(), secondary_road.getY());
                gc.drawImage(road_piece_second.getMyImageView().snapshot(param, null), road_piece_second.getX(),
                        road_piece_second.getY());
                        gc.drawImage(taxi.getMyImageView().snapshot(param, null), taxi.getMyImageView().getX(), taxi.getMyImageView().getY());
                primary_road.setX(primary_road.getX() - speedAdjust);
                road_piece_prime.setX(road_piece_prime.getX() - speedAdjust);
                secondary_road.setX(secondary_road.getX() - speedAdjust);
                road_piece_second.setX(road_piece_second.getX() - speedAdjust);
                if (primary_road.getX() < -1080) {
                    primary_road.setX(1080);
                }
                if (road_piece_prime.getX() < -1080) {
                    road_piece_prime.setX(1080);
                }
                if (secondary_road.getX() < -1080) {
                    secondary_road.setX(1080);
                }
                if (road_piece_second.getX() < -1080) {
                    road_piece_second.setX(1080);
                }

                if (taxi.getMyImageView().getX() > 600) {
                    speedAdjust = 0;
                } else {speedAdjust = 5;}

            }
        };
        loop.start();
    };


    private Image load_image(String path) {
        try (FileInputStream fis = new FileInputStream(path)) {
            return new Image(fis);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Image scale(Image source, int targetWidth, int targetHeight, boolean preserveRatio) {
        ImageView imageView = new ImageView(source);
        imageView.setPreserveRatio(preserveRatio);
        imageView.setFitWidth(targetWidth);
        imageView.setFitHeight(targetHeight);
        return imageView.snapshot(null, null);
    }



}
