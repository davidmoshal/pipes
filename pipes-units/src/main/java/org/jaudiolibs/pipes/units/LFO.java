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
 * Single channel low frequency oscillator unit. Unlike {@link Osc} the
 * waveforms are not bandlimited, and by default the output is between 0 and 1
 * (see {@link #bipolar(boolean)}).
 */
public final class LFO extends OpHolder {

    private final static float DEFAULT_FREQUENCY = 1;
    private final static Waveform DEFAULT_WAVEFORM = Waveform.Sine;

    private final Op op;

    /**
     * Create an LFO unit.
     */
    public LFO() {
        this(new Op());
    }

    private LFO(Op op) {
        super(op, 0, 1);
        this.op = op;
        reset();
    }

    /**
     * Set oscillator frequency in Hz. Default value 1.0.
     *
     * @param frequency oscillator frequency in Hz.
     * @return this for chaining
     */
    public LFO frequency(double frequency) {
        op.setFrequency((float) frequency);
        return this;
    }

    /**
     * Query the oscillator frequency,
     *
     * @return frequency in Hz
     */
    public double frequency() {
        return op.getFrequency();
    }

    /**
     * Set the oscillator waveform. Default {@link Waveform#Sine}.
     *
     * @param waveform type of wave
     * @return this for chaining
     */
    public LFO waveform(Waveform waveform) {
        op.setWaveform(Objects.requireNonNull(waveform));
        return this;
    }

    /**
     * Query the oscillator waveform.
     *
     * @return oscillator waveform
     */
    public Waveform waveform() {
        return op.getWaveform();
    }

    /**
     * Set the oscillator gain. Default 1.0.
     *
     * @param level linear gain
     * @return this for chaining
     */
    public LFO gain(double level) {
        op.setGain((float) level);
        return this;
    }

    /**
     * Query the oscillator gain.
     *
     * @return linear gain
     */
    public double gain() {
        return op.getGain();
    }

    /**
     * Set the oscillator as bipolar - output values between -1.0 and 1.0 rather
     * then between 0.0 and 1.0. Default false.
     *
     * @param bipolar output between -1.0 and 1.0
     * @return this for chaining
     */
    public LFO bipolar(boolean bipolar) {
        op.setBipolar(bipolar);
        return this;
    }

    /**
     * Query whether the oscillator is set to bipolar output.
     *
     * @return bipolar
     */
    public boolean bipolar() {
        return op.getBipolar();
    }

    @Override
    public void reset() {
        op.setFrequency(DEFAULT_FREQUENCY);
        op.setWaveform(Waveform.Sine);
        op.setGain(1);
        op.setBipolar(false);
    }

    private static class Op implements AudioOp {

        private final static float TWOPI = (float) (2 * Math.PI);

        private float phase;
        private float phaseIncrement;
        private float freq;
        private float srate;
        private float gain, oldGain;
        private Waveform wave;
        private boolean bipolar;

        private Op() {
            this.freq = DEFAULT_FREQUENCY;
            this.wave = DEFAULT_WAVEFORM;
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

        public void setBipolar(boolean bipolar) {
            this.bipolar = bipolar;
        }

        public boolean getBipolar() {
            return bipolar;
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
            if (gain != 0 || oldGain != 0) {
                float g = oldGain;
                float delta = (gain - g) / buffersize;
                for (int i = 0; i < buffersize; i++) {
                    out[i] = g * nextSample();
                    g += delta;
                }
                oldGain = gain;
            } else {
                Arrays.fill(out, 0);
                phase += (phaseIncrement * buffersize);
                while (phase >= TWOPI) {
                    phase -= TWOPI;
                }
            }
        }

        @Override
        public void processAdd(int buffersize, float[][] outputs, float[][] inputs) {
            float[] out = outputs[0];
            if (gain != 0 || oldGain != 0) {
                float g = oldGain;
                float delta = (gain - g) / buffersize;
                for (int i = 0; i < buffersize; i++) {
                    out[i] += g * nextSample();
                    g += delta;
                }
                oldGain = gain;
            } else {
                phase += (phaseIncrement * buffersize);
                while (phase >= TWOPI) {
                    phase -= TWOPI;
                }
            }
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
                    break;
                case Square:
                    if (phase < Math.PI) {
                        value = 1.0f;
                    } else {
                        value = -1.0f;
                    }
                    break;
            }
            if (!bipolar) {
                value = (value / 2) + 0.5f;
            }
            phase += phaseIncrement;
            while (phase >= TWOPI) {
                phase -= TWOPI;
            }
            return value;
        }

    }

}
