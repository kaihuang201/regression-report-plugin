package jp.skypencil.jenkins.regression;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.HashMap;

import hudson.model.AbstractBuild;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.ClassResult;
import hudson.tasks.junit.PackageResult;
import hudson.tasks.junit.TestResult;

import hudson.tasks.test.AbstractTestResultAction;
import hudson.tasks.test.AggregatedTestResultAction;
import hudson.tasks.junit.TestResultAction;

public class RegressionReportHelper {

    /** 
     * Returns a list of CaseResults that are contained in a build. Currently this
     * function only handles builds whose getResult return 
     * an object of type TestResultAction or AggregatedTestResultAction
     * @param build an AbstractBuild object from which the caller wants to get
     *      the case results.
     * @return a List of CaseResult.
     */
    public static List<CaseResult> getAllCaseResultsForBuild(AbstractBuild build) {
        List<CaseResult> ret = new ArrayList<CaseResult>();
        List<AbstractTestResultAction> testActions = build.getActions(AbstractTestResultAction.class);

        for (AbstractTestResultAction testAction : testActions) {
            if (testAction instanceof TestResultAction) {
                hudson.tasks.junit.TestResult testResult = (hudson.tasks.junit.TestResult) testAction.getResult();
                ret.addAll(getTestsFromTestResult(testResult));
            }
            else if (testAction instanceof AggregatedTestResultAction) {
                List<AggregatedTestResultAction.ChildReport> child_reports = ((AggregatedTestResultAction)testAction).getChildReports();
                for (AggregatedTestResultAction.ChildReport child_report: child_reports) {
                    hudson.tasks.junit.TestResult testResult = (hudson.tasks.junit.TestResult) child_report.result;
                    ret.addAll(getTestsFromTestResult(testResult));
                }
            }
            else {
                System.out.println("Unsupported project type.");
            }
        }
        return ret;
    }

    /**
     * A helper function that returns a CaseResult from a TestResult object.
     * @param testResult a TestResult object that contains PackageResult as its
     *      children.
     * @return A List of CaseResult.
     */
    private static List<CaseResult> getTestsFromTestResult(TestResult testResult) {
        List<CaseResult> tests = new ArrayList<CaseResult>();
        Collection<PackageResult> packageResults = testResult.getChildren();
        for (PackageResult packageResult : packageResults) {
            Collection<ClassResult> classResults = packageResult.getChildren();
            for (ClassResult classResult : classResults) {
                Collection<CaseResult> caseResults = classResult.getChildren();
                tests.addAll(caseResults);
            }
        }

        return tests;
    }


    /**
     * This function returns a list of Pairs of matching CaseResult, given thisBuild and otherBuild
     * Each pair is of form (CaseResultFromThisBuild, CaseResultFromThatBuild)
     *
     * @param thisBuild an AbstractBuild.
     * @param otherBuild another AbstractBuild, which is compared against thisBuild
     * @return a List of Pairs of CaseResults.Each pair is of form 
     * (CaseResultFromThisBuild, CaseResultFromThatBuild)
     * if a matching case result is not found in the other build, a null is used
     * instead.
     */
    public static List<Pair<CaseResult, CaseResult>> matchTestsBetweenBuilds(AbstractBuild thisBuild, AbstractBuild otherBuild) {
        List<CaseResult> thisResults = getAllCaseResultsForBuild(thisBuild);
        List<CaseResult> otherResults = getAllCaseResultsForBuild(otherBuild);

        HashMap<String, CaseResult> hmap = new HashMap<String, CaseResult>();
        for (CaseResult otherCaseResult : otherResults) {
            hmap.put(otherCaseResult.getFullName(), otherCaseResult); // add (test_name, CaseResult) to hmap
        }

        List<Pair<CaseResult, CaseResult>> returnValue = new ArrayList<Pair<CaseResult, CaseResult>>();
        for (CaseResult thisCaseResult : thisResults) {
            String currTestName = thisCaseResult.getFullName();
            CaseResult otherCaseResult = null;
            if (hmap.containsKey(currTestName)) {
                otherCaseResult = hmap.get(currTestName);
                hmap.remove(currTestName);
            }
            Pair pair = new Pair<CaseResult, CaseResult>(thisCaseResult, otherCaseResult);
            returnValue.add(pair);
        }

        for (CaseResult otherCaseResultInMap : hmap.values()) {
            Pair pair = new Pair<CaseResult, CaseResult>(null, otherCaseResultInMap);
            returnValue.add(pair);
        }

        return returnValue;
    }
    
}
