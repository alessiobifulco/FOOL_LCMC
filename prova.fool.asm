push 0
lhp
push function0
lhp
sw
lhp
push 1
add
shp
push function1
lhp
sw
lhp
push 1
add
shp
push 10
lhp
sw
lhp
push 1
add
shp
push 10000
push -2
add
lw
lhp
sw
lhp
lhp
push 1
add
shp
lfp
push 5
lfp
push -3
add
lw
stm
ltm
ltm
lw
push 1
add
lw
js
push 15
beq label0
push 0
b label1
label0:
push 1
label1:
print
halt

function0:
cfp
lra
lfp
lw
push -1
add
lw
stm
sra
pop
sfp
ltm
lra
js

function1:
cfp
lra
lfp
lw
push -1
add
lw
lfp
push 1
add
lw
add
stm
sra
pop
pop
sfp
ltm
lra
js