#!/usr/bin/env python3
"""Original TileBloom short sound effect synthesis. No external samples.
Assets and synthesis dedicated to CC0-1.0. Requires Python stdlib only.
"""
import math, random, wave, struct
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT/'app/src/main/res/raw'
RATE = 22050
random.seed(37)
def render(name, duration, notes):
    data = [0.] * round(duration*RATE)
    noise = 0.
    for start, freq, length, gain in notes:
        for i in range(round(length*RATE)):
            t = i/RATE
            envelope = min(1, t/.012)*math.exp(-t/(length*.25))*min(1,(length-t)/.06)
            value = gain*envelope*(math.sin(2*math.pi*freq*t)+.22*math.sin(2*math.pi*freq*2.003*t)+.07*math.sin(2*math.pi*freq*3*t))
            if name in ('reveal','hide','select'):
                noise = .65*noise + .35*random.uniform(-1,1)
                value += gain*noise*math.exp(-t/(.025 if name=='select' else .065))*min(1,t/.004)*.65
            data[(round(start*RATE)+i)%len(data)] += value
    peak=max(abs(x) for x in data) or 1
    scale=.68/peak
    path=OUT/(name+'.wav')
    with wave.open(str(path),'wb') as w:
        w.setparams((1,2,RATE,0,'NONE','not compressed'))
        w.writeframes(b''.join(struct.pack('<h',round(x*scale*32767)) for x in data))
for name, frequencies, length in [
    ('button',[620],.055),('select',[520,780],.10),('reveal',[620,830],.20),('hide',[500,350],.16),
    ('match',[1046.5,1318.51,1567.98],.55),('hint',[783.99,1046.5,1567.98],.65),
    ('undo',[740,554,440],.28),('restart',[392,523.25],.38),
    ('victory',[523.25,659.25,783.99,1046.5],1.1),('defeat',[523.25,440,349.23],.9)]:
    render(name,length+len(frequencies)*.06,[(i*.06,f,length,.35) for i,f in enumerate(frequencies)])
