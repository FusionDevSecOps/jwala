/**
 * A dialog box container that is positioned in the page hence the word "static" in the name.
 */
var RStaticDialog = React.createClass({
    render: function() {
        return React.createElement("div", {className: "ui-dialog ui-widget ui-widget-content ui-front " + this.props.className},
                   React.createElement("div", {className: "ui-dialog-titlebar ui-widget-header ui-helper-clearfix"},
                       React.createElement("span", {className: "ui-dialog-title text-align-center"}, this.props.title)),
                   React.createElement("div", {className: "ui-dialog-content ui-widget-content"}, this.props.children));
    }
});