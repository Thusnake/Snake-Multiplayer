package thusnake.snakemultiplayer.textures;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.support.annotation.NonNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import thusnake.snakemultiplayer.Entity;
import thusnake.snakemultiplayer.OpenGLActivity;
import thusnake.snakemultiplayer.Snake;
import thusnake.snakemultiplayer.SnakeSkin;

/**
 * Produces a texture map that holds all the textures to be used in a particular game.
 * Texture coordinates in the map can then be accessed during the game.
 */
public class GameTextureMap extends TextureMap {
  private Map<SnakeSkin, TextureMapCoordinates> snakeTextures = new HashMap<>();
  private Map<Entity, TextureMapCoordinates> entityTextures = new HashMap<>();
  boolean usingSharedTexture = false;

  /**
   * Initializes a texture map for a game.
   * @param snakes A list of the participating snakes.
   * @param entities A list of the registered entities.
   */
  public GameTextureMap(List<Snake> snakes, List<Entity> entities) {
    addAllSnakes(snakes);
    addAllEntities(entities);
  }

  /**
   * Initializes a small texture map for a skin showcase.
   * @param skin A single skin to be added.
   */
  public GameTextureMap(SnakeSkin skin) {
    addSnakeSkin(skin);
  }

  /**
   * Adds a snake skin to the texture map by adding it to the combined bitmap.
   * @param skin The skin to be added.
   */
  public void addSnakeSkin(SnakeSkin skin) {
    if (snakeTextures.containsKey(skin)) return;

    if (textureMap == null) {
      // There is no map yet.
      textureMap = OpenGLActivity.current.getRenderer()
                                         .loadTextureBitmap(skin.textureId());
      snakeTextures.put(skin, new TextureMapCoordinates(0, textureMap));
      usingSharedTexture = true;
    }
    else {
      // We have to concatenate.
      Bitmap snakeBitmap = OpenGLActivity.current.getRenderer()
                                .loadTextureBitmap(skin.textureId());
      Bitmap combinedBitmap = Bitmap.createBitmap(textureMap.getWidth() + snakeBitmap.getWidth(),
          Math.max(textureMap.getHeight(), snakeBitmap.getHeight()), Bitmap.Config.ARGB_8888);
      Canvas combinedCanvas = new Canvas(combinedBitmap);
      float heightDiff = snakeBitmap.getHeight() - textureMap.getHeight();
      combinedCanvas.drawBitmap(textureMap, 0f, heightDiff > 0f ? heightDiff : 0f, null);
      combinedCanvas.drawBitmap(snakeBitmap, textureMap.getWidth(), heightDiff < 0f ? -heightDiff : 0, null);

      // Save the offset at which we've concatenated.
      snakeTextures.put(skin, new TextureMapCoordinates(textureMap.getWidth(), snakeBitmap));

      // Apply result.
      Bitmap oldTextureMap = textureMap;
      textureMap = combinedBitmap;

      // Recycle old bitmap (if not shared in the cache).
      if (!usingSharedTexture)
        oldTextureMap.recycle();

      usingSharedTexture = false;
    }
  }

  /**
   * Adds a whole list of players' skins to the texture map.
   * @param snakes The list of players to be added.
   */
  public void addAllSnakes(List<Snake> snakes) {
    for (Snake snake : snakes) {
      addSnakeSkin(snake.getSkin());
    }
  }

  /**
   * Adds an entity to the texture map by adding its texture to the combined bitmap.
   * @param entity The entity to be added.
   */
  public void addEntity(Entity entity) {
    if (entityTextures.containsKey(entity)) return;

    if (textureMap == null) {
      // There is no map yet.
      textureMap = OpenGLActivity.current.getRenderer().loadTextureBitmap(entity.textureId);
      entityTextures.put(entity, new TextureMapCoordinates(0, textureMap));
      usingSharedTexture = true;
    }
    else {
      // We have to concatenate.
      Bitmap entityBitmap = OpenGLActivity.current.getRenderer()
                                                  .loadTextureBitmap(entity.textureId);
      Bitmap combinedBitmap = Bitmap.createBitmap(textureMap.getWidth() + entityBitmap.getWidth(),
          Math.max(textureMap.getHeight(), entityBitmap.getHeight()), Bitmap.Config.ARGB_8888);
      Canvas combinedCanvas = new Canvas(combinedBitmap);
      float heightDiff = entityBitmap.getHeight() - textureMap.getHeight();
      combinedCanvas.drawBitmap(textureMap, 0f, heightDiff > 0f ? heightDiff : 0f, null);
      combinedCanvas.drawBitmap(entityBitmap, textureMap.getWidth(), heightDiff < 0f ? -heightDiff : 0f, null);

      // Save the offset at which we've concatenated.
      entityTextures.put(entity, new TextureMapCoordinates(textureMap.getWidth(), entityBitmap));

      // Apply result.
      Bitmap oldTextureMap = textureMap;
      textureMap = combinedBitmap;

      // Recycle old bitmap (if not shared in the cache).
      if (!usingSharedTexture)
        oldTextureMap.recycle();

      usingSharedTexture = false;
    }
  }

  /**
   * Adds a whole list of entities to the texture map.
   * @param entities The list of entities to be added.
   */
  public void addAllEntities(List<Entity> entities) {
    for (Entity entity : entities) {
      addEntity(entity);
    }
  }

  /**
   * @return A given snake's skin's part's texture coordinates in a given direction from this
   *          texture map.
   */
  public TextureMapCoordinates getTexture(@NonNull SnakeSkin skin,
                                          @NonNull SnakeSkin.TextureType part,
                                          @NonNull Snake.Direction direction) {
    TextureMapCoordinates snakeCoords = snakeTextures.get(skin);
    TextureMapCoordinates partCoords = skin.getCoordinates(part, direction);

    return new TextureMapCoordinates(snakeCoords.offsetX + partCoords.offsetX,
                                     snakeCoords.offsetY + partCoords.offsetY,
                                     partCoords.width,
                                     partCoords.height);
  }

  /** @return A given entity's texture coordinates in this texture map. */
  public TextureMapCoordinates getTexture(Entity entity) {
    return entityTextures.get(entity);
  }

  @Override
  public void recycle() {
    if (!usingSharedTexture)
      textureMap.recycle();
    textureMap = null;
  }
}
