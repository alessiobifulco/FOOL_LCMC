push 0
push 10
push 5
lfp
push -2
add
lw
push 2
div
lfp
push -3
add
lw
beq label0
push 0
b label1
label0:
push 1
label1:
print
halt