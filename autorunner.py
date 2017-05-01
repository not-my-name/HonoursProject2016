import os.path
import os
import time

pid = input("Enter the process ID: ")

procPath = "/proc/" + str(pid)

while os.path.exists(procPath):
	time.sleep(3) #checks every hour if the process is still running 

os.chdir("/home/ruben/HonoursContinuedTests/HyperNEAT/Novelty/HonoursProject2016/")
os.system("./gradlew run")
