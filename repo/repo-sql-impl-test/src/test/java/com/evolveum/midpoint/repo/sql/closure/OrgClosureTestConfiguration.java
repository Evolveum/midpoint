/*
 * Copyright (c) 2010-2014 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.sql.closure;

/**
 * @author mederly
 */
public class OrgClosureTestConfiguration {

    // whether to check closure matrix by retrieving it from DB and comparing with freshly-computed one
    // (unusable for big closure tables, e.g. 10K+ entries)
    private boolean checkClosureMatrix;

    // whether to check closure table by comparing descendants retrieved from m_org_closure with the ones
    // computed from the graph in memory; it is done for each graph node (may be slow for big closure tables)
    private boolean checkChildrenSets;

    // how meny orgs to create at given tree level for each parent-level org
    // (so, the number at level 0 indicates how many parent-level orgs is there)
    private int[] orgChildrenInLevel;

    // how many users to create at given tree level for each parent-level org
    private int[] userChildrenInLevel;

    // how many parents should an org/user residing at given level have
    // (for level 0 there should be zero; all other items should be non-zero)
    private int[] parentsInLevel;

    // how many times should be add/remove link test be carried out for a given level
    // (for level 0 there should be no such test, i.e. 0-th element should always be 0)
    private int[] linkRoundsForLevel;

    // how many times should be add/remove node test be carried out for a given level
    private int[] nodeRoundsForLevel;

    // each (what number) of deletions in test410 should be the closure tested
    private int deletionsToClosureTest;

    public boolean isCheckClosureMatrix() {
        return checkClosureMatrix;
    }

    public void setCheckClosureMatrix(boolean checkClosureMatrix) {
        this.checkClosureMatrix = checkClosureMatrix;
    }

    public boolean isCheckChildrenSets() {
        return checkChildrenSets;
    }

    public void setCheckChildrenSets(boolean checkChildrenSets) {
        this.checkChildrenSets = checkChildrenSets;
    }

    public int[] getOrgChildrenInLevel() {
        return orgChildrenInLevel;
    }

    public void setOrgChildrenInLevel(int[] orgChildrenInLevel) {
        this.orgChildrenInLevel = orgChildrenInLevel;
    }

    public int[] getUserChildrenInLevel() {
        return userChildrenInLevel;
    }

    public void setUserChildrenInLevel(int[] userChildrenInLevel) {
        this.userChildrenInLevel = userChildrenInLevel;
    }

    public int[] getParentsInLevel() {
        return parentsInLevel;
    }

    public void setParentsInLevel(int[] parentsInLevel) {
        this.parentsInLevel = parentsInLevel;
    }

    public int[] getLinkRoundsForLevel() {
        return linkRoundsForLevel;
    }

    public void setLinkRoundsForLevel(int[] linkRoundsForLevel) {
        this.linkRoundsForLevel = linkRoundsForLevel;
    }

    public int[] getNodeRoundsForLevel() {
        return nodeRoundsForLevel;
    }

    public void setNodeRoundsForLevel(int[] nodeRoundsForLevel) {
        this.nodeRoundsForLevel = nodeRoundsForLevel;
    }

    public int getDeletionsToClosureTest() {
        return deletionsToClosureTest;
    }

    public void setDeletionsToClosureTest(int deletionsToClosureTest) {
        this.deletionsToClosureTest = deletionsToClosureTest;
    }
}
