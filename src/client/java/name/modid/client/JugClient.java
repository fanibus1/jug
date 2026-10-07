package name.modid.client;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

import net.minecraft.client.Minecraft;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import com.mojang.authlib.minecraft.client.MinecraftClient;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;

import name.modid.client.mixin.MinecraftAccessor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

public class JugClient implements ClientModInitializer {
    // TODO: dont hardcode ts ass
    private static int n0 = 58; // A4
    String penisHardcode = "/Users/afan/Developer/mc/jug/san.mid";

    private static final String[] NOTE_NAMES = {
            "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"
    };

    private volatile boolean isPlaying = false;
    private Thread playbackThread = null;

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            dispatcher.register(
                    ClientCommandManager.literal("setp")
                            .then(ClientCommandManager.argument("pitch", FloatArgumentType.floatArg(-90.0F, 90.0F))
                                    .executes(this::executeSetPitch)));
            // start/play midi
            dispatcher.register(ClientCommandManager.literal("penis").executes(this::executePenis));
            dispatcher.register(ClientCommandManager.literal("stopmidi").executes(this::executeStopMidi));

            // set start note
            dispatcher.register(
                    ClientCommandManager.literal("setn0")
                            .then(ClientCommandManager.argument("n0_new", IntegerArgumentType.integer())
                                    .executes(this::setStartNote)));
        });
    }

    private int setStartNote(CommandContext<FabricClientCommandSource> context) {
        int n0_new = IntegerArgumentType.getInteger(context, "n0_new");

        sendMessageInGame("Changed n0 from " + n0 + " to " + n0_new);
        n0 = n0_new;
        return Command.SINGLE_SUCCESS;
    }

    private int executeSetPitch(CommandContext<FabricClientCommandSource> context) {
        float pitch = FloatArgumentType.getFloat(context, "pitch");
        CameraUtil.setPlayerPitch(pitch);
        sendMessageInGame("Changed pitch to " + pitch);
        return Command.SINGLE_SUCCESS;
    }

    private int executePenis(CommandContext<FabricClientCommandSource> context) {
        // Stop any currently running playback first
        stopPlayback();

        isPlaying = true;
        playbackThread = new Thread(() -> tryExecuteMidi(penisHardcode), "MidiPlaybackThread");
        playbackThread.start();
        return Command.SINGLE_SUCCESS;
    }

    private int executeStopMidi(CommandContext<FabricClientCommandSource> context) {
        stopPlayback();
        sendMessageInGame("§cMIDI playback stopped.");
        return Command.SINGLE_SUCCESS;
    }

    private void stopPlayback() {
        isPlaying = false;
        if (playbackThread != null && playbackThread.isAlive()) {
            playbackThread.interrupt();
        }
    }

    private void sendMessageInGame(String text) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            client.player.sendSystemMessage(Component.literal(text));
        }
    }

    /**
     * Converts a MIDI note number (0-127) into a scientific pitch string (e.g., 69
     * -> "A4").
     * MIDI note 0 is C-1, note 60 is C4, note 69 is A4.
     */
    private String getNoteNameWithOctave(int midiNote) {
        String noteName = NOTE_NAMES[midiNote % 12];
        int octave = (midiNote / 12) - 1;
        return noteName + octave;
    }

    private float mapMidiToPitch(int midiNote) {
        int noteRel = midiNote - n0;

        // base note mul is 0.5, max note rel will be 2.0 (need to clamp)
        double freqMul = Math.pow(2.0f, noteRel / 12.0f - 1.0f);

        double pitch = Math.toDegrees(Math.asin(-2 * (freqMul - 0.8)));

        return (float) pitch;
    }

    private long tickToMs(long ticks, int resolution, long mpqn) {
        // resolution = ticks per quarter note
        // mpqn = microseconds per quarter note (default 500,000 = 120 BPM)
        return (ticks * mpqn) / (resolution * 1000L);
    }

    private void perfUseItem() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.gameMode == null)
            return;

        // Dispatch to the main client thread
        client.execute(() -> {
            ((MinecraftAccessor) client).invokeStartUseItem();
        });
    }

    private int tryExecuteMidi(String pathToFile) {
        try {
            File midiFile = new File(pathToFile);
            Sequence sequence = MidiSystem.getSequence(midiFile);
            int resolution = sequence.getResolution();
            long mpqn = 500000; // Default tempo: 120 BPM (500,000 µs/beat)

            // 1. Scan for tempo meta events (type 0x51) across all tracks
            for (Track track : sequence.getTracks()) {
                for (int i = 0; i < track.size(); i++) {
                    MidiMessage msg = track.get(i).getMessage();
                    if (msg instanceof MetaMessage meta && meta.getType() == 0x51) {
                        byte[] data = meta.getData();
                        mpqn = ((data[0] & 0xFF) << 16) | ((data[1] & 0xFF) << 8) | (data[2] & 0xFF);
                        break;
                    }
                }
            }

            // 2. Flatten all events from all tracks into a single chronologically sorted
            // list
            List<MidiEvent> allEvents = new ArrayList<>();
            for (Track track : sequence.getTracks()) {
                for (int i = 0; i < track.size(); i++) {
                    allEvents.add(track.get(i));
                }
            }
            allEvents.sort(Comparator.comparingLong(MidiEvent::getTick));

            // 3. Play back events with per-tick deduplication
            long lastEventTick = 0;
            Set<Integer> notesTriggeredThisTick = new HashSet<>();

            for (int i = 0; i < allEvents.size(); i++) {
                MidiEvent event = allEvents.get(i);
                MidiMessage message = event.getMessage();
                long tick = event.getTick();

                if (message instanceof ShortMessage sm) {
                    int command = sm.getCommand();
                    int note = sm.getData1();
                    int velocity = sm.getData2();
                    int channel = sm.getChannel();

                    // Optional: Skip percussion channel 10 (0-indexed: 9) if not needed
                    // if (channel == 9) continue;

                    boolean isNoteOn = (command == ShortMessage.NOTE_ON && velocity > 0);
                    boolean isNoteOff = (command == ShortMessage.NOTE_OFF)
                            || (command == ShortMessage.NOTE_ON && velocity == 0);

                    if (isNoteOn || isNoteOff) {
                        // When the tick advances, sleep and reset the active chord deduplicator
                        if (tick > lastEventTick) {
                            long waitMs = tickToMs(tick - lastEventTick, resolution, mpqn);
                            if (waitMs > 0) {
                                Thread.sleep(waitMs);
                            }
                            lastEventTick = tick;
                            notesTriggeredThisTick.clear();
                        }

                        if (isNoteOn) {
                            // Skip if this exact note already triggered at this tick (prevents duplicate
                            // tracks/channels)
                            if (!notesTriggeredThisTick.add(note)) {
                                continue;
                            }

                            // Find matching NOTE_OFF event forward in time to measure hold duration
                            long holdTicks = 0;
                            for (int j = i + 1; j < allEvents.size(); j++) {
                                MidiEvent futureEvent = allEvents.get(j);
                                if (futureEvent.getMessage() instanceof ShortMessage futureSm) {
                                    boolean endOfThisNote = (futureSm.getData1() == note) &&
                                            (futureSm.getCommand() == ShortMessage.NOTE_OFF ||
                                                    (futureSm.getCommand() == ShortMessage.NOTE_ON
                                                            && futureSm.getData2() == 0));
                                    if (endOfThisNote) {
                                        holdTicks = futureEvent.getTick() - tick;
                                        break;
                                    }
                                }
                            }

                            long holdTimeMs = tickToMs(holdTicks, resolution, mpqn);
                            float pitch = mapMidiToPitch(note);
                            String noteName = getNoteNameWithOctave(note);

                            System.out.printf("MIDI Note: %3d | Note: %-4s | Hold Time: %5d ms | Pitch: %6.2f%n",
                                    note, noteName, holdTimeMs, pitch);
                            sendMessageInGame("MIDI - pitch: " + pitch + " note: " + note + " ( " + noteName + " ) for "
                                    + holdTimeMs);

                            // Fire pitch change and item use
                            CameraUtil.setPlayerPitch(pitch);
                            perfUseItem();
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Command.SINGLE_SUCCESS;
    }
}