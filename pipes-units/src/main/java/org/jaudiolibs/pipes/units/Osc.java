/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2019 Neil C Smith.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * version 3 for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License version 3
 * along with this work; if not, see http://www.gnu.org/licenses/
 *
 */
package org.jaudiolibs.pipes.units;

import java.util.Arrays;
import java.util.Objects;
import org.jaudiolibs.audioops.AudioOp;
import org.jaudiolibs.pipes.OpHolder;

/**
 * A single channel oscillator unit. The waveforms are bandlimited, where
 * appropriate, for audio use.
 */
public final class Osc extends OpHolder {

    private final static float DEFAULT_FREQUENCY = 440;

    private final Op op;

    /**
     * Create an Osc unit.
     */
    public Osc() {
        this(new Op());
    }

    Osc(Op op) {
        super(op, 1);
        this.op = op;
        reset();
    }

    /**
     * Set the oscillator frequency in Hz. Default 440.
     *
     * @param frequency oscillator frequency in Hz
     * @return this for chaining
     */
    public Osc frequency(double frequency) {
        op.setFrequency((float) frequency);
        return this;
    }

    /**
     * Query the oscillator frequency.
     *
     * @return oscillator frequency in Hz
     */
    public double frequency() {
        return op.getFrequency();
    }

    /**
     * Set the {@link Waveform} of the oscillator. Default
     * {@link Waveform#Sine}.
     *
     * @param waveform oscillator waveform
     * @return this for chaining
     */
    public Osc waveform(Waveform waveform) {
        op.setWaveform(Objects.requireNonNull(waveform));
        return this;
    }

    /**
     * Query the waveform of the oscillator.
     *
     * @return waveform
     */
    public Waveform waveform() {
        return op.getWaveform();
    }

    /**
     * Set the gain of the oscillator. Default 1.0.
     *
     * @param level linear gain
     * @return this for chaining
     */
    public Osc gain(double level) {
        op.setGain((float) level);
        return this;
    }

    /**
     * Query the gain of the oscillator.
     *
     * @return linear gain
     */
    public double gain() {
        return op.getGain();
    }

    @Override
    public void reset() {
        op.setFrequency(DEFAULT_FREQUENCY);
        op.setWaveform(Waveform.Sine);
        op.setGain(1);
    }

    private static class Op implements AudioOp {

        private final static float TWOPI = (float) (2 * Math.PI);

        private float phase;
        private float phaseIncrement;
        private float freq;
        private float srate;
        private float gain, oldGain;
        private Waveform wave;

        private Op() {
            this.freq = DEFAULT_FREQUENCY;
            this.wave = Waveform.Sine;
            this.gain = 1;
        }

        public void setGain(float gain) {
            this.gain = gain;
        }

        public float getGain() {
            return gain;
        }

        public void setFrequency(float frequency) {
            this.freq = frequency;
            updateIncrement();
        }

        public float getFrequency() {
            return this.freq;
        }

        public void setWaveform(Waveform mode) {
            this.wave = mode;
        }

        public Waveform getWaveform() {
            return wave;
        }

        @Override
        public void initialize(float samplerate, int buffersize) {
            this.srate = samplerate;
            this.phase = 0;
            updateIncrement();
        }

        @Override
        public void reset(int i) {
            phase = 0; // increment phase using i
        }

        @Override
        public boolean isInputRequired(boolean bln) {
            return false;
        }

        @Override
        public void processReplace(int buffersize, float[][] outputs, float[][] inputs) {
            float[] out = outputs[0];
            float g1 = oldGain;
            float g2 = freq > 1 ? gain : 0;
            if (g1 != 0 || g2 != 0) {
                float delta = (g2 - g1) / buffersize;
                for (int i = 0; i < buffersize; i++) {
                    out[i] = g1 * nextSample();
                    g1 += delta;
                }
            } else {
                Arrays.fill(out, 0);
                phase += (phaseIncrement * buffersize);
                while (phase >= TWOPI) {
                    phase -= TWOPI;
                }
            }
            oldGain = g2;
        }

        @Override
        public void processAdd(int buffersize, float[][] outputs, float[][] inputs) {
            float[] out = outputs[0];
            float g1 = oldGain;
            float g2 = freq > 1 ? gain : 0;
            if (g1 != 0 || g2 != 0) {
                float delta = (g2 - g1) / buffersize;
                for (int i = 0; i < buffersize; i++) {
                    out[i] += g1 * nextSample();
                    g1 += delta;
                }
            } else {
                phase += (phaseIncrement * buffersize);
                while (phase >= TWOPI) {
                    phase -= TWOPI;
                }
            }
            oldGain = g2;
        }

        private void updateIncrement() {
            float inc = 0;
            if (srate > 0) {
                inc = TWOPI * freq / srate;
                if (inc < 0) {
                    inc = 0;
                }
            }
            phaseIncrement = inc;
        }

        private float nextSample() {
            float value = 0;
            float t = phase / TWOPI;
            switch (wave) {
                case Sine:
                    value = (float) Math.sin(phase);
                    break;
                case Saw:
                    value = (2.0f * t) - 1.0f;
                    value -= polyBLEP(t);
                    break;
                case Square:
                    if (phase < Math.PI) {
                        value = 1.0f;
                    } else {
                        value = -1.0f;
                    }

                    value += polyBLEP(t);
                    value -= polyBLEP((t + 0.5f) % 1.0f);
                    break;
            }
            phase += phaseIncrement;
            while (phase >= TWOPI) {
                phase -= TWOPI;
            }
            return value;
        }

        private float polyBLEP(float t) {
            float dt = phaseIncrement / TWOPI;
            if (t < dt) {
                t /= dt;
                return t + t - t * t - 1.0f;
            } else if (t > 1.0f - dt) {
                t = (t - 1.0f) / dt;
                return t * t + t + t + 1.0f;
            } else {
                return 0;
            }

        }
    }

}
