import sys
import random 
import numpy

n = int(sys.argv[1])

beta = 1000
fout = open('data/exponential.csv','w')
for i in range(n):        
    x = numpy.random.exponential(beta)
    fout.write('%d\n'%x)
fout.close()

alpha = 1.001
fout = open('data/zipfian.csv','w')
for i in range(n):
    x = int(numpy.random.zipf(alpha))
    fout.write('%d\n'%x)
fout.close()

fout = open('data/uniform.csv','w')
for i in range(n):
    x = random.randint(0,n-1)
    fout.write('%d\n'%x)
fout.close()

k = 1000
fout = open('data/planted.csv','w')
for i in range(n):
    x = random.randint(0,k-1) if random.random()<0.5 else random.randint(0,n-1)     
    fout.write('%d\n'%x)
fout.close()