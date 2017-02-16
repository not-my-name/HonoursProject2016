import os.path
import os
import time

pid = input("Enter the process ID: ")

procPath = "/proc/" + str(pid)

while os.path.exists(procPath):
	time.sleep(3600) #checks every hour if the process is still running 

os.chdir("/home/p/pttand010/Desktop/Experiments_Rerun/Novelty/Run_1/HonoursProject2016")
os.system("rm -rf ~/.gradle/caches")
os.system("pwd")
os.system("./gradlew run")
