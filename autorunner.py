import os.path
import os
import time

pid = input("Enter the process ID: ")

procPath = "/proc/" + str(pid)

while os.path.exists(procPath):
	time.sleep(1) #checks every 5 minutes if the process is still running 

print("The thing has finished")
os.system("cd /home/ruben/HonoursContinuedTests/HyperNEAT/Novelty")
os.system("./gradlew run")
