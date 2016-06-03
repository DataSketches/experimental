import random 
import numpy

n = 10000000

fout = open('data/exponential_n=%d_beta=%d.csv'%(n,beta),'w')
beta = 1000
sys.stderr.write('Creating exponential data\n')
for i in range(n):        
    x = numpy.random.exponential(beta)
    fout.write('%d\n'%x)
fout.close()

fout = open('data/zipfian_n=%d_alpha=%f.csv'%(n,alpha),'w')
alpha = 1.001
sys.stderr.write('Creating zipfian data\n')
for i in range(n):
    x = int(numpy.random.zipf(alpha))
    fout.write('%d\n'%x)
fout.close()

fout = open('data/uniform_n=%d.csv'%n,'w')
for i in range(n):
    x = random.randint(0,n-1)
    fout.write('%d\n'%x)
fout.close()

sys.stderr.write('Creating planted data\n')
k = 1000
fout = open('data/planted_n=%d_k=%d.csv'%(n,k),'w')
for i in range(n):
    x = random.randint(0,k-1) if random.random()<0.5 else random.randint(0,n-1)     
    fout.write('%d\n'%x)
fout.close()

