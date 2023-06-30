#!/usr/bin/python3
from cmath import phase
from ctypes import sizeof
import os
import sched
import sys
import time
from os import path
from subprocess import call
from typing import List
# from TT import getState, clearLogFile, repairNodeState


def writePreFailNodeStateDict(preFailNodeStateDict):
    fileName = "/home/xie/explorer-server/test/preFailNodeStateDict.txt"
    clearLogFile(fileName)
    f = open(fileName, 'a')
    for kv in preFailNodeStateDict.items():
        f.write(str(kv) + "\n")
    f.close()


def getPreFailNodeStateDict():
    fileName = "/home/xie/explorer-server/test/preFailNodeStateDict.txt"
    f = open(fileName, 'r')
    data = f.read()
    testList = data.split("\n")
    testList = testList[0: len(testList) - 1]

    dic = dict()

    for line in testList:
        ll = list(eval(line))
        dic[ll[0]] = ll[1]
    return dic

def clearFile(filePath):
    with open(filePath, "r+") as f:
        read_data = f.read()
        f.seek(0)
        f.truncate()
    f.close()

def writeScenarios(failNodeList, filePath):
    # 先清空在写入
    with open(filePath, "r+") as f:
        read_data = f.read()
        f.seek(0)
        f.truncate()
        for fid in failNodeList:
            f.write(fid + '\n')
    f.close()

def collectAndWrite(phase, round, failNodeList):
    failNodeStateFile = "/Users/xieyiyang/Desktop/ecnu/zoo-State-Merge/test/"
    state = "Phase" + str(phase - 1) + "Round" + str(round) + " "

    aimFile = failNodeStateFile + state
    # 将所有 failnode 拼起来
    failNodeName = ""
    for fnd in failNodeList:
        failNodeName += "_" + fnd

    # 得到三个节点的 state 并且拼起来
    state = "Phase" + str(phase - 1) + "Round" + str(round) + " "
    stateFilePath = "/Users/xieyiyang/Desktop/ecnu/zoo-State-Merge/test/states/"
    keyNum = phase + 2
    if round == 0:
        keyWord = "INIT"
    elif round == 1:
        keyWord = "PROPOSE"
    else:
        keyWord = "COMMIT"
    
    for i in range(3):
        sfp = stateFilePath + str(i)
        with open(sfp, 'r') as ff:
            lines = ff.readlines()
            cnt = 0
            isFind = False
            for line in lines:
                if keyWord in line:
                    cnt += 1
                    if cnt == keyNum:
                        state += line
                        break
            if not isFind:
                state += "null"

    if not os.path.exists(aimFile):
        os.system(r"touch {}".format(aimFile))
    f = open(aimFile, 'a')
    f.write(failNodeName + "|" + str(state) + "\n")
    f.close()

def clearStateFile():
    stateFile = "/Users/xieyiyang/Desktop/ecnu/zoo-State-Merge/test/states/"
    for i in range(3):
        aimFile = stateFile + str(i)
        clearFile(aimFile)

def main():

    # if len(scheduler.split(".")) <= 0:
    #     print("Please enter a valid scheduler name: e.g. explorer.scheduler.NodeFailureInjector")
    #     return
    scenario = "scenario-1"
    totalPhase = 2
    totalRound = 3


    scenarioFile = "/Users/xieyiyang/Desktop/ecnu/zoo-State-Merge/test/scenarios"
    failNodeStateFile = "/Users/xieyiyang/Desktop/ecnu/zoo-State-Merge/test/"
    stateFile = "/Users/xieyiyang/Desktop/ecnu/zoo-State-Merge/test/states/"

    resultFile = "/home/xie/explorer-server/test/result.txt"
    preFailNodeId = []
    preFailStateNodeDict = dict()
    preFailNodeStateDict = dict()
    tmpStateDict = dict()

    startFromHalf = True

    # nodeid round phase
    for i in range(1, totalPhase + 1):
        for j in range(totalRound):
            # 如果是 p1 r0
            if i == 1 and j == 0:
                # 确定 drop 的 node
                curFailNodeList = []
                for k in range(8):
                    if k == 0:
                        curFailNodeList.append(str(0) + " " + str(j) + " " + str(i))
                    elif k == 1:
                        curFailNodeList.append(str(1) + " " + str(j) + " " + str(i))
                    elif k == 2:
                        curFailNodeList.append(str(2) + " " + str(j) + " " + str(i))
                    elif k == 3:
                        curFailNodeList.append(str(0) + " " + str(j) + " " + str(i))
                        curFailNodeList.append(str(1) + " " + str(j) + " " + str(i))
                    elif k == 4:
                        curFailNodeList.append(str(0) + " " + str(j) + " " + str(i))
                        curFailNodeList.append(str(2) + " " + str(j) + " " + str(i))
                    elif k == 5:
                        curFailNodeList.append(str(1) + " " + str(j) + " " + str(i))
                        curFailNodeList.append(str(2) + " " + str(j) + " " + str(i))
                    elif k == 6:
                        curFailNodeList.append(str(0) + " " + str(j) + " " + str(i))
                        curFailNodeList.append(str(1) + " " + str(j) + " " + str(i))
                        curFailNodeList.append(str(2) + " " + str(j) + " " + str(i))
                    else:
                        failNodeId = ""

                    # 更新scenarios
                    writeScenarios(curFailNodeList, scenarioFile)

                    # 清空3个节点的 stateFile
                    clearStateFile()
                    # 运行
                    call("mvn {0} {1} {2}".format("exec:java", "-Dexec.mainClass=edu.upenn.zootester.ZooTester",
                                                  "-Dexec.args=\"-s {0}\" ".format(scenario)), shell=True)

                    # 分别收集3个节点当前 round 状态，写入到对应的文件中

                    collectAndWrite(i, j, curFailNodeList)

                    # 清空3个节点的 stateFile
                    clearStateFile()
            else:
                return
                # 读取上一轮的 failnode - state, 并同时进行去重


if __name__ == '__main__':
    os.chdir("..")
    # main()
    scenario = "scenario-1"
    call("mvn {0} {1} {2}".format("exec:java", "-Dexec.mainClass=edu.upenn.zootester.ZooTester",
                                  "-Dexec.args=\"-s {0}\" ".format(scenario)), shell=True)
