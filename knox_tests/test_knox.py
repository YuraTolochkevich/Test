#
#
# Copyright  (c) 2011-2013, Hortonworks Inc.  All rights reserved.  
#
#
# Except as expressly permitted in a written Agreement between your 
# company and Hortonworks, Inc, any use, reproduction, modification, 
# redistribution or other exploitation of all or any part of the contents 
# of this file is strictly prohibited.
#
#
from beaver.component.hadoop import Hadoop
from beaver.config import Config
from beaver import util
import os, collections
import shutil
import pytest
from beaver.maven import Maven

SRC_DIR = os.path.join(Config.getEnv('WORKSPACE'), 'tests', 'knox')
LOCAL_WORK_DIR = os.path.join(Config.getEnv('ARTIFACTS_DIR'), 'knox')
TESTCASES = []
TEST_RESULT = {}
def generate_tests():
    #copy the knox testsuite to artifacts dir
    shutil.copytree(SRC_DIR, LOCAL_WORK_DIR)

    # use maven opts to set the command line options as it fails on windows otherwise
    exit_code, stdout = Maven.run('clean test', cwd=LOCAL_WORK_DIR)
    testresult = {}
    # get a list of all the test result files
    testResultFiles = util.findMatchingFiles(os.path.join(LOCAL_WORK_DIR, 'target', 'surefire-reports'), 'TEST-*.xml')
    for resultFile  in testResultFiles:
      testresult.update(util.parseJUnitXMLResult(resultFile))
    for key, value in testresult.items():
        TEST_RESULT[key] = value
    TESTCASES.extend(sorted(TEST_RESULT.keys()))

# generate_tests()
# 
# @pytest.mark.parametrize("tcid", TESTCASES)
# def test_knox(tcid):
#     assert TEST_RESULT[tcid]['result'] == "pass", TEST_RESULT[tcid]['failure']
