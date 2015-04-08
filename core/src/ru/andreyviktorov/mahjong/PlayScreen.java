package ru.andreyviktorov.mahjong;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.ui.Window.WindowStyle;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class PlayScreen implements Screen {
    public static Stage stage;
    final Mahjong game;
    public static float SCALE_RATIO = 128 / Gdx.graphics.getWidth();
    public static float TILE_WIDTH;
    public static float TILE_HEIGHT;
    public static GameData gamedata;
    public static List<List<Image>> shadows = new ArrayList();
    public Texture shadowtexture;
    public Texture glowtexture;
    public static Image glowimg;
    public static Image[][][] shadowimgs;
    private static List<String> backgrounds;

    public static Group back;
    public static Group fore;

    public static Label remainLabel;
    public static Label availableLabel;

    public PlayScreen (final Mahjong gameref) {
        game = gameref;

        stage = new Stage(new ScreenViewport());

        // TODO: сделать загрузку геймдаты из сейва
        gamedata = new GameData();
        gamedata.scaleModificator = Generator.getFigureHeight(Generator.Figure.Pyramid) - 6.5F;
        System.out.println("СМ:" + gamedata.scaleModificator);

        Generator gen = new Generator();
        gamedata.field = gen.generateFigure(Generator.Figure.Pyramid);

        glowtexture = new Texture(Gdx.files.internal("data/tiles/TileSelected.png"));
        glowimg = new Image(glowtexture);
        glowimg.setPosition(-500, -500);
        shadowimgs = new Image[gamedata.field.getWidth()+1][gamedata.field.getHeight()+1][gamedata.field.layers.size()+1];

        shadowtexture = new Texture(Gdx.files.internal("data/tiles/TileShadow.png"));
        shadowtexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        back = new Group();
        back.setBounds(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        fore = new Group();
        fore.setBounds(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        stage.addActor(back);
        stage.addActor(fore);

        backgrounds = new ArrayList();

        backgrounds.add("data/backgrounds/1-leaves.jpg");
        backgrounds.add("data/backgrounds/2-leaves.jpg");
        backgrounds.add("data/backgrounds/3-leaves.jpg");

        Collections.shuffle(backgrounds);

        gamedata.background = new Image(new Texture(Gdx.files.internal(backgrounds.get(0))));

        back.addActor(gamedata.background);

        Image topbar = new Image(new Texture(Gdx.files.internal("data/ui.png")));
        topbar.setColor(0,0,0,0.3F);
        float barheight = Gdx.graphics.getHeight()/10;
        topbar.setSize(Gdx.graphics.getWidth(), barheight);
        topbar.setPosition(0, Gdx.graphics.getHeight() - barheight);

        back.addActor(topbar);

        Label.LabelStyle ls = new Label.LabelStyle();
        ls.font = game.fontsHash.get("semi-big");
        remainLabel = new Label("Осталось фишек: 144", ls);
        remainLabel.setPosition(Gdx.graphics.getWidth()/48,
                Gdx.graphics.getHeight() - Utils.getCenteredHeight(barheight, remainLabel.getHeight()));

        availableLabel = new Label("Возможных ходов: " + countAvailablePairs(), ls);

        float apos = Gdx.graphics.getWidth() - Gdx.graphics.getWidth()/50 - availableLabel.getWidth();

        availableLabel.setPosition(apos, Gdx.graphics.getHeight() - Utils.getCenteredHeight(barheight, availableLabel.getHeight()));

        back.addActor(remainLabel);
        back.addActor(availableLabel);

        // TODO: сделать нормальный стиль для диалога
        TextureRegion tr = new TextureRegion(new Texture(Gdx.files.internal("data/ui.png")));
        TextureRegionDrawable WTFBLYAT = new TextureRegionDrawable(tr);
        TextButton.TextButtonStyle tbs = new TextButton.TextButtonStyle(WTFBLYAT, WTFBLYAT, WTFBLYAT, game.fontsHash.get("small"));

        Dialog dia = new Dialog("Правила игры", new WindowStyle(game.fontsHash.get("semi-medium"), Color.WHITE, WTFBLYAT));
        dia.pad(50, 10, 10, 10);
        dia.text("Добро пожаловать в пасьянс маджонг!\r\nКраткие правила игры:\r\nНужно убрать с поля все парные фишки.\r\nФишки делятся на два типа: обычные и джокеры.\r\nДжокеры - это фишки с цифрой в левом верхнем углу, и убираются опираясь на картинку в центре.\r\n\r\nФишки не могут быть убраны если:\r\n1. Над ней есть другая фишка\r\n2. Слева и справа от неё есть другие фишки", new Label.LabelStyle(game.fontsHash.get("small"), Color.WHITE));
        dia.button("OK", true, tbs);

        dia.show(stage);

        // TODO: сделать нормальный стиль для кнопки
        TextButton shuffleButton = new TextButton("Перемешать", tbs);
        back.addActor(shuffleButton);

        float fieldWidth;
        float fieldHeight;
        float fieldPosX;
        float fieldPosY;

        // z-index
        float g_offset_y = 0;
        int layernumber = 0;
        List<TileActor> layerPool;
        List<Image> shadowPool;
        for(Layer layer : gamedata.field.layers) {
            layerPool = new LinkedList();
            shadowPool = new LinkedList();
            for(int i = 0; i<gamedata.field.getWidth(); i++) {
                for(int j = 0; j<gamedata.field.getHeight(); j++) {
                    if(layer.data[i][j] != null) {
                        // Получаем шириновысоту
                        /*System.out.println(layer.data[i][j].getHeight());
                        TILE_WIDTH = layer.data[i][j].getWidth();
                        TILE_HEIGHT = layer.data[i][j].getHeight();*/

                        // +2 для отступов
                        fieldWidth = TILE_WIDTH * ((gamedata.field.getWidth() + 2) / 2) - TILE_WIDTH;
                        fieldHeight = TILE_HEIGHT * ((gamedata.field.getHeight() + 2) / 2) - TILE_HEIGHT;


                        //System.out.println(layer.data[i][j].tile.suit.name());

                        //System.out.println("TILE_W " + TILE_WIDTH + " TILE_H " + TILE_HEIGHT + " FW " + fieldWidth + " FH " + fieldHeight);

                        fieldPosX = Gdx.graphics.getWidth()/2 - fieldWidth / 2;
                        fieldPosY = Gdx.graphics.getHeight()/2 - fieldHeight / 2;

                        layer.data[i][j].tiledata.datax = i;
                        layer.data[i][j].tiledata.datay = j;
                        layer.data[i][j].tiledata.layer = layernumber;
                        
                        layer.data[i][j].tiledata.x = fieldPosX + TILE_WIDTH * i/2;
                        layer.data[i][j].tiledata.y = fieldPosY + TILE_HEIGHT * j/2 - g_offset_y;

                        Image img = new Image(shadowtexture);
                        // Еще одна магическая константа: 102/148
                        img.setSize(Gdx.graphics.getHeight() * 12 / 100 * 0.68918918918F, Gdx.graphics.getHeight() * 12 / 100);
                        float offsetx = (img.getWidth() - TILE_WIDTH) / 2;
                        float offsety = (img.getHeight() - TILE_HEIGHT) / 2;

                        img.setPosition(fieldPosX + TILE_WIDTH * i/2 - offsetx, fieldPosY + TILE_HEIGHT * j/2 - g_offset_y - offsety);

                        shadowimgs[i][j][layernumber] = img;

                        shadowPool.add(shadowimgs[i][j][layernumber]);
                        layerPool.add(layer.data[i][j]);
                        //stage.addActor(shadowimgs[i][j][layernumber]);
                        //stage.addActor(layer.data[i][j]);
                    }
                }
            }

            // Flushing pools
            for(Image img : shadowPool) {
                fore.addActor(img);
            }
            for(TileActor act : layerPool) {
                fore.addActor(act);
            }

            System.out.println("Finished layer " + layernumber);
            layernumber++;
            g_offset_y += TILE_HEIGHT/5.8;
        }

        // Must be on top of any else
        fore.addActor(glowimg);

        Gdx.input.setInputProcessor(stage);
        Gdx.input.setCatchBackKey(true);
    }

    public static int countAvailablePairs() {
        List<TileActor> avalist = new ArrayList();
        Field f = PlayScreen.gamedata.field;
        int n = 0;
        for(Layer l : f.layers) {
            for(int i = 0; i<f.getWidth() + 1; i++) {
                for(int j = 0; j<f.getHeight() + 1; j++) {
                    if(l.data[i][j] != null) {
                        if(f.canRemove(n, i, j)) {
                            avalist.add(l.data[i][j]);
                        }
                    }
                }
            }
            n++;
        }

        int c = 0;

        for(TileActor one : avalist) {
            for(TileActor two : avalist) {
                if((one.randomId != two.randomId) && ((one.tile.suit == two.tile.suit && one.tile.number == two.tile.number) || (one.tile.suit == two.tile.suit && one.tile.suit == Tile.Suit.Season) || (one.tile.suit == two.tile.suit && one.tile.suit == Tile.Suit.Flower))) {
                    c++;
                }
            }
        }

        return c/2;
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(1, 1, 1, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
    }

    @Override
    public void show() {

    }

    @Override
    public void hide() {
        this.dispose();
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void dispose() {
        this.stage.dispose();
        this.stage = null;
    }
}
