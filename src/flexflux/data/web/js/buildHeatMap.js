$(document).ready(function() {
 var inchlib = new InCHlib({
                    target: "inchlib",
                    max_height: 1200,
                    width: 1000,
                    independent_columns: false
                });
                inchlib.read_data(data);
                inchlib.draw();
});