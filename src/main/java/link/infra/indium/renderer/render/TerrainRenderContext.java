public class TerrainRenderContext extends AbstractBlockRenderContext {
    private final ChunkVertexEncoder.Vertex[] vertices = ChunkVertexEncoder.Vertex.uninitializedQuad();

    private ChunkBuildBuffers buffers;
    private boolean didOutput = false;

    private Vector3fc origin;
    private Vec3d modelOffset;

    // Variáveis para evitar alocações repetitivas
    private static final String CRASH_SECTION = "Block being tessellated";
    private final ModelQuadFacing tmpCullFace = ModelQuadFacing.UNASSIGNED;
    private final ModelQuadOrientation tmpOrientation = ModelQuadOrientation.FLIP;
    private final ChunkVertexEncoder.Vertex[] tmpVertices = new ChunkVertexEncoder.Vertex[4];
    private final MutableQuadViewImpl tmpQuad = new MutableQuadViewImpl();

    public TerrainRenderContext(BlockRenderCache renderCache) {
        // Restante do código igual
    }

    // Restante do código igual

    /** Called from chunk renderer hook. */
    public boolean tessellateBlock(BlockRenderContext ctx) {
        try {
            // Restante do código igual
            this.origin = ctx.origin();
            this.modelOffset = ctx.state().getModelOffset(ctx.world(), ctx.pos());

            didOutput = false;
            aoCalc.clear();
            blockInfo.prepareForBlock(ctx.state(), ctx.pos(), ctx.seed(), ctx.model().useAmbientOcclusion());

            // Reutilizar o array de vértices e quad temporário
            ChunkModelBuilder builder = buffers.get(renderLayer);
            IndexBufferBuilder indexBuffer = builder.getIndexBuffer(tmpCullFace);

            for (int dstIndex = 0; dstIndex < 4; dstIndex++) {
                int srcIndex = tmpOrientation.getVertexIndex(dstIndex);

                ChunkVertexEncoder.Vertex out = tmpVertices[dstIndex];
                out.x = origin.x() + tmpQuad.x(srcIndex) + (float) modelOffset.getX();
                out.y = origin.y() + tmpQuad.y(srcIndex) + (float) modelOffset.getY();
                out.z = origin.z() + tmpQuad.z(srcIndex) + (float) modelOffset.getZ();

                int color = tmpQuad.color(srcIndex);
                out.color = ColorARGB.toABGR(color, (color >>> 24) & 0xFF);

                out.u = tmpQuad.u(srcIndex);
                out.v = tmpQuad.v(srcIndex);

                out.light = tmpQuad.lightmap(srcIndex);
            }

            indexBuffer.add(builder.getVertexBuffer().push(tmpVertices), ModelQuadWinding.CLOCKWISE);

            Sprite sprite = tmpQuad.cachedSprite();

            if (sprite == null) {
                sprite = SpriteFinderCache.forBlockAtlas().find(tmpQuad);
            }

            builder.addSprite(sprite);
        } catch (Throwable throwable) {
            CrashReport crashReport = CrashReport.create(throwable, "Tessellating block in world - Indium Renderer");
            CrashReportSection crashReportSection = crashReport.addElement(CRASH_SECTION);
            CrashReportSection.addBlockInfo(crashReportSection, ctx.world(), ctx.pos(), ctx.state());
            throw new CrashException(crashReport);
        }

        return didOutput;
    }
	}
