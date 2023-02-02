/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

function network_graph_auth(jsonObjectList, jsonObjectList2) {
    let nodes = new vis.DataSet(
        JSON.parse(jsonObjectList)
    );

    console.log(JSON.parse(jsonObjectList))
    let edges = new vis.DataSet(JSON.parse(jsonObjectList2)
    );

    let data = {
        nodes: nodes,
        edges: edges
    };
    const options = {

        layout: {
            hierarchical: {
                direction: 'UD',
                sortMethod: 'directed',
                levelSeparation: 400,
                nodeSpacing: 400
            },
        },
        physics: false
    };
    let container = document.getElementById("network_graph_auth");

    new vis.Network(container, data, options);

}

//
// function network_graph_role(labelsNames, datasetValues) {
//     let nodes = new vis.DataSet([
//
//         {"id":"1","label":"[1, 2, 3, 4]"},
//         {"id":"2","label":"[1, 2, 3]"},
//         {"id":"3","label":"[1, 2, 4]"},
//         {"id":"4","label":"[1, 3, 4]"},
//         {"id":"5","label":"[2, 3, 4]"},
//         {"id":"6","label":"[1, 2]"},
//         {"id":"7","label":"[1, 3]"},
//         {"id":"8","label":"[2, 3]"},
//         {"id":"9","label":"[1, 4]"},
//         {"id":"10","label":"[2, 4]"},
//         {"id":"11","label":"[3, 4]"},
//         {"id":"12","label":"[1]"},
//         {"id":"13","label":"[2]"},
//         {"id":"14","label":"[3]"},
//         {"id":"15","label":"[4]"},
//     ]);
//
//     console.log(nodes);
//     let edges = new vis.DataSet([
//         {"from":"1","to":"2"},
//         {"from":"1","to":"3"},
//         {"from":"1","to":"4"},
//         {"from":"1","to":"5"},
//         {"from":"2","to":"6"},
//         {"from":"2","to":"7"},
//         {"from":"2","to":"8"},
//         {"from":"3","to":"6"},
//         {"from":"3","to":"9"},
//         {"from":"3","to":"10"},
//         {"from":"4","to":"7"},
//         {"from":"4","to":"9"},
//         {"from":"4","to":"11"},
//         {"from":"5","to":"8"},
//         {"from":"5","to":"10"},
//         {"from":"5","to":"11"},
//         {"from":"6","to":"12"},
//         {"from":"6","to":"13"},
//         {"from":"7","to":"12"},
//         {"from":"7","to":"14"},
//         {"from":"8","to":"13"},
//         {"from":"8","to":"14"},
//         {"from":"9","to":"12"},
//         {"from":"9","to":"15"},
//         {"from":"10","to":"13"},
//         {"from":"10","to":"15"},
//         {"from":"11","to":"14"},
//         {"from":"11","to":"15"},
//     ]);
//
//     let data = {
//         nodes: nodes,
//         edges: edges
//     };
//     var options = {
//         layout: {
//             hierarchical: {
//                 direction: "UD",
//                 sortMethod: "directed",
//             },
//         },
//     };
//     let container = document.getElementById("network_graph_role");
//
//     let network = new vis.Network(container, data, options);
//
// }
