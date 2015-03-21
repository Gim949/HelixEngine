/*
 * Copyright (C) 2014, 2015 Helix Engine Developers
 * (http://github.com/fauu/HelixEngine)
 *
 * This software is licensed under the GNU General Public License
 * (version 3 or later). See the COPYING file in this distribution.
 *
 * You should have received a copy of the GNU Library General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 *
 * Authored by: Piotr Grabowski <fau999@gmail.com>
 */

package com.github.fauu.helix.system;

import com.artemis.ComponentMapper;
import com.artemis.Entity;
import com.artemis.annotations.Wire;
import com.artemis.managers.TagManager;
import com.artemis.systems.VoidEntitySystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Vector3;
import com.github.fauu.helix.Direction;
import com.github.fauu.helix.component.*;
import com.github.fauu.helix.datum.Tile;
import com.github.fauu.helix.spatial.Spatial;
import com.github.fauu.helix.util.IntVector2;
import com.github.fauu.helix.util.IntVector3;

import java.util.HashMap;
import java.util.Map;

public class PlayerMovementSystem extends VoidEntitySystem {

  @Wire
  private TagManager tagManager;

  @Wire
  private ComponentMapper<MovementSpeedComponent> movementSpeedMapper;

  @Wire
  private ComponentMapper<OrientationComponent> orientationMapper;

  @Wire
  private ComponentMapper<PositionComponent> positionMapper;

  @Wire
  private ComponentMapper<SpatialFormComponent> spatialFormMapper;

  @Wire
  private ComponentMapper<TilesComponent> tilesMapper;

  @Wire
  private PerspectiveCamera camera;

  private static final float MOVEMENT_START_DELAY;

  private static final HashMap<Direction, Integer> DIRECTION_KEYS;

  private boolean moving;

  private float movementStartDelayCounter;

  private float movementProgressCounter;

  static {
    MOVEMENT_START_DELAY = 0.1f;

    DIRECTION_KEYS = new HashMap<Direction, Integer>();
    DIRECTION_KEYS.put(Direction.NORTH, Input.Keys.W);
    DIRECTION_KEYS.put(Direction.EAST, Input.Keys.D);
    DIRECTION_KEYS.put(Direction.SOUTH, Input.Keys.S);
    DIRECTION_KEYS.put(Direction.WEST, Input.Keys.A);
  }

  @Override
  protected void processSystem() {
    Entity player = tagManager.getEntity("player");

    Direction orientation = orientationMapper.get(player).get();

    Spatial spatial = spatialFormMapper.get(player).get();

    float movementSpeed =  movementSpeedMapper.get(player).get();

    float movementDuration = 1 / movementSpeed;

    Direction requestedMovementDirection = null;

    for (Map.Entry<Direction, Integer> dk : DIRECTION_KEYS.entrySet()) {
      if (Gdx.input.isKeyPressed(dk.getValue())) {
        requestedMovementDirection = dk.getKey();
        break;
      }
    }

    if (!moving) {
      if (requestedMovementDirection == null) {
        movementStartDelayCounter = 0;
      } else {
        if (movementStartDelayCounter == 0) {
          orientationMapper.get(player).set(requestedMovementDirection);
          spatial.update(Spatial.UpdateType.ORIENTATION,
                         requestedMovementDirection);
        }

        movementStartDelayCounter += Gdx.graphics.getDeltaTime();

        if (movementStartDelayCounter >= MOVEMENT_START_DELAY) {
          IntVector3 position = positionMapper.get(player).get();
          IntVector2 targetPosition
              = position.toIntVector2()
                        .add(requestedMovementDirection.getVector());

          Tile[][] tiles = tilesMapper.get(tagManager.getEntity("area")).get();
          Tile currentTile = tiles[position.x][position.y];
          Tile targetTile = tiles[targetPosition.x][targetPosition.y];
          if (targetTile.getPermissions() == currentTile.getPermissions()) {
            moving = true;
            movementStartDelayCounter = 0;

            spatial.update(Spatial.UpdateType.PLAY_ANIMATION, orientation);

            positionMapper.get(player)
                          .set(new IntVector3(targetPosition.x,
                                              targetPosition.y,
                                              targetTile.getPermissions()
                                                      .getElevation()));
          }
        } // end "if movementStartDelayCounter >= MOVEMENT_START_DELAY"
      } // end "if requestedMovementDirection != null"
    } // end "if !moving"

    if (moving) {
      Direction movementDirection = orientation;

      if (movementProgressCounter >= movementDuration) {
        moving = false;
        movementProgressCounter = 0;

        spatial.update(Spatial.UpdateType.IDLE, orientation);
      } else {
        float delta = Gdx.graphics.getDeltaTime();

        if (movementProgressCounter + delta > movementDuration) {
          delta = movementDuration - movementProgressCounter;
        }

        movementProgressCounter += Gdx.graphics.getDeltaTime();

        IntVector2 movementDirectionVector = movementDirection.getVector();

        Vector3 translation = new Vector3(movementDirectionVector.x,
                                          movementDirectionVector.y,
                                          0);
        translation.scl(delta * movementSpeed);

        spatial.update(Spatial.UpdateType.POSITION, translation);

        camera.translate(translation);

        if (movementProgressCounter >= movementDuration &&
            requestedMovementDirection != null) {
          movementStartDelayCounter = MOVEMENT_START_DELAY;

          spatial.update(Spatial.UpdateType.ORIENTATION,
                         requestedMovementDirection);

          orientationMapper.get(player).set(requestedMovementDirection);
        }
      } // end "if movementProgressCounter < movementDuration"
    } // end "if moving"
  }

}