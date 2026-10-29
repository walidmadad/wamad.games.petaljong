#!/usr/bin/env python3
"""Original TileBloom score and procedural timbres. No samples or external music.
Assets and this score dedicated to CC0-1.0. Requires Python stdlib and ffmpeg.
"""
import math, random, wave, struct, subprocess
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT/'app/src/main/res/raw'
RATE = 22050
random.seed(37)
def render(name, duration, notes, ambient=False):
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
    if ambient:
        for i in range(len(data)):
            t=i/RATE
            data[i] += .025*math.sin(2*math.pi*130.8128*t)*math.sin(math.pi*t/duration)**2
    if ambient:
        # Gentle phrase breathing also protects the compressed decoder boundary.
        fade = round(.35*RATE)
        for i in range(fade):
            gain = math.sin(math.pi*.5*i/fade)**2
            data[i] *= gain
            data[-1-i] *= gain
    peak=max(abs(x) for x in data) or 1
    scale=.68/peak
    path=OUT/(name+'.wav')
    with wave.open(str(path),'wb') as w:
        w.setparams((1,2,RATE,0,'NONE','not compressed'))
        w.writeframes(b''.join(struct.pack('<h',round(x*scale*32767)) for x in data))
    if ambient:
        subprocess.run(['ffmpeg','-v','error','-y','-i',str(path),'-c:a','libvorbis','-q:a','4',str(OUT/(name+'.ogg'))],check=True)
        path.unlink()
for name, frequencies, length in [
    ('select',[520,780],.10),('reveal',[620,830],.20),('hide',[500,350],.16),
    ('match',[1046.5,1318.51,1567.98],.55),('hint',[783.99,1046.5,1567.98],.65),
    ('undo',[740,554,440],.28),('restart',[392,523.25],.38),
    ('victory',[523.25,659.25,783.99,1046.5],1.1),('defeat',[523.25,440,349.23],.9)]:
    render(name,length+len(frequencies)*.06,[(i*.06,f,length,.35) for i,f in enumerate(frequencies)])
# 48 seconds, slow C / Am / F / G pentatonic garden phrases. Tails wrap
# modulo the phrase to make the decoded PCM boundary continuous.
notes=[]
chords=[[261.6256,329.6276,391.9954],[220,261.6256,329.6276],
        [174.6141,261.6256,349.2282],[195.9977,293.6648,391.9954]]
for bar in range(8):
    chord=chords[bar%4]
    for step in range(4):
        notes.append((bar*6+step*1.5,chord[(step+bar)%3]* (2 if step==3 else 1),5,.16))
    notes.append((bar*6,chord[0]/2,8,.09))
render('zen_garden',48,notes,True)
