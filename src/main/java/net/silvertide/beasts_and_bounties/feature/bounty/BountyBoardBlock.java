package net.silvertide.beasts_and_bounties.feature.bounty;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class BountyBoardBlock extends HorizontalDirectionalBlock {
    private static final MapCodec<BountyBoardBlock> CODEC = simpleCodec(BountyBoardBlock::new);
    private static final int BOARD_BOTTOM_PIXELS = 8;
    private static final VoxelShape NORTH_SOUTH_SHAPE = Shapes.or(Block.box(7, 0, 7, 9, BOARD_BOTTOM_PIXELS, 9), Block.box(0, BOARD_BOTTOM_PIXELS, 7, 16, 16, 9));
    private static final VoxelShape EAST_WEST_SHAPE = Shapes.or(Block.box(7, 0, 7, 9, BOARD_BOTTOM_PIXELS, 9), Block.box(7, BOARD_BOTTOM_PIXELS, 0, 9, 16, 16));

    BountyBoardBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<BountyBoardBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FACING).getAxis() == Direction.Axis.X ? EAST_WEST_SHAPE : NORTH_SOUTH_SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        boolean hitTheBoard = hitResult.getLocation().y - pos.getY() >= BOARD_BOTTOM_PIXELS / 16.0;
        if (!hitTheBoard) return InteractionResult.PASS;
        if (player instanceof ServerPlayer serverPlayer) PlayerBounties.openBoard(serverPlayer);
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
