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

