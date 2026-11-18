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
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class BountyBoardBlock extends HorizontalDirectionalBlock {
    private static final MapCodec<BountyBoardBlock> CODEC = simpleCodec(BountyBoardBlock::new);
    private static final int BOARD_BOTTOM_PIXELS = 8;
    private static final VoxelShape NORTH_SOUTH_SHAPE = Shapes.or(Block.box(7, 0, 7, 9, BOARD_BOTTOM_PIXELS, 9), Block.box(0, BOARD_BOTTOM_PIXELS, 7, 16, 16, 9));
    private static final VoxelShape EAST_WEST_SHAPE = Shapes.or(Block.box(7, 0, 7, 9, BOARD_BOTTOM_PIXELS, 9), Block.box(7, BOARD_BOTTOM_PIXELS, 0, 9, 16, 16));
    private static final int WALL_BOARD_BOTTOM_PIXELS = 4;
    private static final int WALL_BOARD_TOP_PIXELS = 12;
    private static final VoxelShape WALL_NORTH_SHAPE = Block.box(0, WALL_BOARD_BOTTOM_PIXELS, 14, 16, WALL_BOARD_TOP_PIXELS, 16);
    private static final VoxelShape WALL_SOUTH_SHAPE = Block.box(0, WALL_BOARD_BOTTOM_PIXELS, 0, 16, WALL_BOARD_TOP_PIXELS, 2);
    private static final VoxelShape WALL_EAST_SHAPE = Block.box(0, WALL_BOARD_BOTTOM_PIXELS, 0, 2, WALL_BOARD_TOP_PIXELS, 16);
    private static final VoxelShape WALL_WEST_SHAPE = Block.box(14, WALL_BOARD_BOTTOM_PIXELS, 0, 16, WALL_BOARD_TOP_PIXELS, 16);
    static final BooleanProperty WALL = BooleanProperty.create("wall");

    BountyBoardBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(WALL, false));
    }

    @Override
    protected MapCodec<BountyBoardBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, WALL);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clickedFace = context.getClickedFace();
        if (clickedFace.getAxis().isHorizontal()) {
            BlockState wallState = defaultBlockState().setValue(FACING, clickedFace).setValue(WALL, true);
            if (wallState.canSurvive(context.getLevel(), context.getClickedPos())) return wallState;
        }
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.relative(supportDirection(state))).isSolid();
    }

    private static Direction supportDirection(BlockState state) {
        return state.getValue(WALL) ? state.getValue(FACING).getOpposite() : Direction.DOWN;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction == supportDirection(state) && !state.canSurvive(level, pos)) return Blocks.AIR.defaultBlockState();
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(WALL)) {
            return switch (state.getValue(FACING)) {
                case SOUTH -> WALL_SOUTH_SHAPE;
                case EAST -> WALL_EAST_SHAPE;
                case WEST -> WALL_WEST_SHAPE;
                default -> WALL_NORTH_SHAPE;
            };
        }
        return state.getValue(FACING).getAxis() == Direction.Axis.X ? EAST_WEST_SHAPE : NORTH_SOUTH_SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        boolean hitTheBoard = state.getValue(WALL) || hitResult.getLocation().y - pos.getY() >= BOARD_BOTTOM_PIXELS / 16.0;
        if (!hitTheBoard) return InteractionResult.PASS;
        if (player instanceof ServerPlayer serverPlayer) PlayerBounties.openBoard(serverPlayer);
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
