/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

function JavaArrayToJsArray(array) {
    return array.replace(/[\[\]]/g, ' ').split(", ");
}

function jaccard_chart(labelsNames, datasetValues, jaccardSortedMatrix, inputThreshold) {

    let datasetData = JSON.parse(datasetValues);
    let datasetLabel = JavaArrayToJsArray(labelsNames);
    let matrix = JSON.parse(jaccardSortedMatrix);
    let jaccardThreshold = inputThreshold;

    new Chart("barChart", {
        type: 'bar',
        data: {
            labels: datasetLabel,
            datasets: [{
                label: 'Intersection index',
                data: datasetData,
                backgroundColor: 'rgb(81,140,184)',
                borderRadius: 1,
                borderSkipped: false,
            }]
        }, options: {
            maintainAspectRatio: false,
            onClick: (event, elements, chart) => {

                let dataIndex = elements[0].index;
                let allData = matrix;
                let threshold = jaccardThreshold;
                let result = [];

                let row = allData[dataIndex];
                for (let j = 0; j < row.length; j++) {
                    if (row[j] >= threshold) {
                        result.push(j);
                    }
                }

                if (elements.length) {
                    const dataset = chart.data.datasets[0];
                    dataset.backgroundColor = [];
                    for (let i = 0; i < dataset.data.length; i++) {
                        if (result.includes(i)) {
                            dataset.backgroundColor[i] = 'rgb(32,32,32)';
                        } else {
                            dataset.backgroundColor[i] = 'rgb(81,140,184)'
                        }
                    }
                    chart.update();
                }
            },

            plugins: {
                legend: {
                    labels: {
                        color: "black",
                        font: {
                            size: 12
                        }
                    }
                }
            },

            scales: {


                y: {
                    display: true,
                    title: {
                        display: true,
                        text: 'SUM',
                        color: '#000000',
                        font: {
                            family: 'Times',
                            size: 15,
                            weight: 'bold',
                            lineHeight: 1.2
                        }
                    },
                    grid: {
                        color: 'black',
                        drawBorder: true,
                        borderColor: 'black'
                    },
                    ticks: {
                        color: 'black',
                        font: {
                            family: 'Times',
                        }
                    }
                },
                x: {
                    axis: 'black',
                    title: {
                        display: true,
                        text: 'USERS',
                        color: 'black',
                        font: {
                            family: 'Times',
                            size: 15,
                            weight: 'bold',
                            lineHeight: 1.2,
                        }
                    },

                    grid: {
                        color: '#000000',
                        display: false,
                        borderColor: 'black'
                    },
                    ticks: {
                        color: 'black',
                        font: {
                            family: 'Times',

                        }
                    }
                }
            },
        }
    });
}
