package xyz.immortius.chunkbychunk.common.blockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import xyz.immortius.chunkbychunk.common.world.SpawnChunkHelper;
import xyz.immortius.chunkbychunk.interop.ChunkByChunkConstants;

import java.util.function.Function;

/**
 * Base class for all chunk spawning block entities. These block entities wait a short period so that entities can spawn
 * in the generation dimension before spawning a chunk.
 */
public abstract class AbstractSpawnChunkBlockEntity extends BlockEntity {

    private static final int TICKS_TO_SPAWN_CHUNK = 1;
    private static final int TICKS_TO_SPAWN_ENTITIES = 20;

    private final Function<BlockPos, ChunkPos> sourceChunkPosFunc;
    private int tickCounter = 0;

    public AbstractSpawnChunkBlockEntity(BlockEntityType<?> blockEntityType, BlockPos pos, BlockState state, Function<BlockPos, ChunkPos> sourceChunkPosFunc) {
        super(blockEntityType, pos, state);
        this.sourceChunkPosFunc = sourceChunkPosFunc;
    }

    public static void serverTick(Level level, BlockPos blockPos, BlockState blockState, AbstractSpawnChunkBlockEntity entity) {
        ServerLevel serverLevel = (ServerLevel) level;
        // If there are no players, entities won't spawn. So don't tick.
        if (serverLevel.getPlayers((p) -> true).isEmpty()) {
            return;
        }
        entity.tickCounter++;
        if (entity.tickCounter == TICKS_TO_SPAWN_CHUNK) {
            ChunkPos targetChunkPos = new ChunkPos(blockPos);
            if (SpawnChunkHelper.isValidForChunkSpawn(serverLevel) && SpawnChunkHelper.isEmptyChunk(serverLevel, targetChunkPos)) {
                SpawnChunkHelper.spawnChunkBlocks(serverLevel, entity.sourceChunkPosFunc.apply(blockPos), targetChunkPos);
            }
        }
        if (entity.tickCounter >= TICKS_TO_SPAWN_ENTITIES) {
            ChunkByChunkConstants.LOGGER.info("Spawning entities");
            if (SpawnChunkHelper.isValidForChunkSpawn(serverLevel)) {
                ChunkPos targetChunkPos = new ChunkPos(blockPos);
                SpawnChunkHelper.spawnChunkEntities(serverLevel, entity.sourceChunkPosFunc.apply(blockPos), targetChunkPos);
            }
            if (serverLevel.getBlockState(blockPos) == blockState) {
                serverLevel.setBlock(blockPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
    }
}
