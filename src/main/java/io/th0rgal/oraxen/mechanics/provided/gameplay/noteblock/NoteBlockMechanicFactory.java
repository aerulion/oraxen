package io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import org.bukkit.Bukkit;
import org.bukkit.Instrument;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NoteBlockMechanicFactory extends MechanicFactory {

    public static final Map<Integer, NoteBlockMechanic> BLOCK_PER_VARIATION = new HashMap<>();
    private static List<JsonObject> blockstateOverrides;
    private static NoteBlockMechanicFactory instance;
    public final List<String> toolTypes;

    public NoteBlockMechanicFactory(ConfigurationSection section) {
        super(section);
        instance = this;
        blockstateOverrides = new ArrayList<>();
        toolTypes = section.getStringList("tool_types");

        // this modifier should be executed when all the items have been parsed, just
        // before zipping the pack
        OraxenPlugin.get().getResourcePack().addModifiers(getMechanicID(),
                packFolder -> {
                    OraxenPlugin.get().getResourcePack()
                            .writeStringToVirtual("assets/minecraft/blockstates",
                                    "note_block.json", getBlockstateContent());
                });
        MechanicsManager.registerListeners(OraxenPlugin.get(), new NoteBlockMechanicListener(this));
    }

    public static String getInstrumentName(int id) {
        return switch (id / 25 % 384) {
            case 1 -> "basedrum";
            case 2 -> "snare";
            case 3 -> "hat";
            case 4 -> "bass";
            case 5 -> "flute";
            case 6 -> "bell";
            case 7 -> "guitar";
            case 8 -> "chime";
            case 9 -> "xylophone";
            case 10 -> "iron_xylophone";
            case 11 -> "cow_bell";
            case 12 -> "didgeridoo";
            case 13 -> "bit";
            case 14 -> "banjo";
            case 15 -> "pling";
            default -> "harp";
        };
    }

    public static JsonObject getBlockstateOverride(String modelName, int id) {
        id += 26;
        return getBlockstateOverride(modelName, getInstrumentName(id), id % 25, id >= 400);
    }

    public static JsonObject getBlockstateOverride(String modelName, String instrument) {
        JsonObject content = new JsonObject();
        JsonObject model = new JsonObject();
        model.addProperty("model", modelName);
        content.add("apply", model);

        JsonObject when = new JsonObject();
        when.addProperty("instrument", instrument);
        content.add("when", when);
        return content;
    }

    public static JsonObject getBlockstateOverride(String modelName, String instrument, int note, boolean powered) {
        JsonObject content = new JsonObject();
        JsonObject model = new JsonObject();
        model.addProperty("model", modelName);
        content.add("apply", model);

        JsonObject when = new JsonObject();
        when.addProperty("instrument", instrument);
        when.addProperty("note", note);
        when.addProperty("powered", powered);
        content.add("when", when);
        return content;
    }

    public static NoteBlockMechanic getBlockMechanic(int customVariation) {
        return BLOCK_PER_VARIATION.get(customVariation);
    }

    public static NoteBlockMechanicFactory getInstance() {
        return instance;
    }


    /**
     * Attempts to set the block directly to the model and texture of an Oraxen item.
     *
     * @param block  The block to update.
     * @param itemId The Oraxen item ID.
     */
    public static void setBlockModel(Block block, String itemId) {
        final MechanicFactory mechanicFactory = MechanicsManager.getMechanicFactory("noteblock");
        NoteBlockMechanic noteBlockMechanic = (NoteBlockMechanic) mechanicFactory.getMechanic(itemId);
        block.setBlockData(createNoteBlockData(noteBlockMechanic.getCustomVariation()), false);
    }

    private String getBlockstateContent() {
        JsonObject noteblock = new JsonObject();
        JsonArray multipart = new JsonArray();
        // adds default override
        multipart.add(getBlockstateOverride("required/note_block", "harp"));
        for (JsonObject override : blockstateOverrides)
            multipart.add(override);
        noteblock.add("multipart", multipart);
        return noteblock.toString();
    }

    @Override
    public Mechanic parse(ConfigurationSection itemMechanicConfiguration) {
        NoteBlockMechanic mechanic = new NoteBlockMechanic(this, itemMechanicConfiguration);
        blockstateOverrides
                .add(getBlockstateOverride(mechanic.getModel(itemMechanicConfiguration.getParent().getParent()),
                        mechanic.getCustomVariation()));
        BLOCK_PER_VARIATION.put(mechanic.getCustomVariation(), mechanic);
        addToImplemented(mechanic);
        return mechanic;
    }

    /**
     * Generate a NoteBlock blockdata from its id
     *
     * @param id The block id.
     */
    @SuppressWarnings("deprecation")
    public static NoteBlock createNoteBlockData(int id) {
        /* We have 16 instruments with 25 notes. All of those blocks can be powered.
         * That's: 16*25*2 = 800 variations. The first 25 variations of PIANO (not powered)
         * will be reserved for the vanilla behavior. We still have 800-25 = 775 variations
         */
        id += 26;
        NoteBlock noteBlock = (NoteBlock) Bukkit.createBlockData(Material.NOTE_BLOCK);
        noteBlock.setInstrument(Instrument.getByType((byte) (id / 25 % 400)));
        noteBlock.setNote(new Note(id % 25));
        noteBlock.setPowered(id >= 400);
        return noteBlock;
    }

    /**
     * Generate a NoteBlock blockdata from an oraxen id
     *
     * @param itemID The id of an item implementing NoteBlockMechanic
     */
    public NoteBlock createNoteBlockData(String itemID) {
        /* We have 16 instruments with 25 notes. All of those blocks can be powered.
         * That's: 16*25*2 = 800 variations. The first 25 variations of PIANO (not powered)
         * will be reserved for the vanilla behavior. We still have 800-25 = 775 variations
         */
        return createNoteBlockData(((NoteBlockMechanic) getInstance().getMechanic(itemID)).getCustomVariation());
    }

}
