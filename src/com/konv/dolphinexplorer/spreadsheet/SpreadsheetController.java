package com.konv.dolphinexplorer.spreadsheet;

import com.konv.dolphinexplorer.DialogHelper;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import org.controlsfx.control.spreadsheet.GridBase;
import org.controlsfx.control.spreadsheet.GridChange;

import java.util.List;

public class SpreadsheetController implements EventHandler<GridChange> {
    private GridBase mGridBase;
    private Graph mGraph;
    private Tokenizer mTokenizer;
    private boolean mShowFormulas = false;

    public SpreadsheetController(GridBase gridBase) {
        mGridBase = gridBase;
        mGraph = new Graph(gridBase.getRowCount(), gridBase.getColumnCount());
        mTokenizer = new Tokenizer();
    }

    @Override
    public void handle(GridChange event) {
        Cell cell = mGraph.getCell(event.getRow(), event.getColumn());
        String formula = (String) event.getNewValue();
        if (formula == null) {
            mGraph.markUnevaluable(cell);
            cell.setFormula("");
            cell.setValue(null);
            display();
            return;
        }
        List<Tokenizer.Token> tokensStream = mTokenizer.tokenize(formula);
        if (tokensStream == null) {
            DialogHelper.showAlert(Alert.AlertType.WARNING, "Spreadsheet", "Invalid input", "Some arguments are not recognized");
        } else if (isSyntaxValid(tokensStream)) {
            cell.setFormula(formula);
            mGraph.resolveDependencies(cell);
            mGraph.evaluate();
        }
        display();
    }

    public void setShowFormulas(boolean showFormulas) {
        mShowFormulas = showFormulas;
    }

    public void display() {
        mGridBase.removeEventHandler(GridChange.GRID_CHANGE_EVENT, this);
        for (int row = 0; row < mGridBase.getRowCount(); ++row) {
            for (int column = 0; column < mGridBase.getColumnCount(); ++column) {
                Cell cell = mGraph.getCell(row, column);
                String representation = cell.toString();
                if (mShowFormulas) representation = cell.getFormula();
                mGridBase.setCellValue(row, column, representation);
            }
        }
        mGridBase.addEventHandler(GridChange.GRID_CHANGE_EVENT, this);
    }

    private boolean isSyntaxValid(List<Tokenizer.Token> tokensStream) {
        if (!SyntaxAnalyzer.isOperatorsBetweenOperands(tokensStream)) {
            DialogHelper.showAlert(Alert.AlertType.WARNING, "Spreadsheet", "Invalid input",
                    "Binary operator is not between two operands");
            return false;
        } else if (!SyntaxAnalyzer.isBracesBalansed(tokensStream)) {
            DialogHelper.showAlert(Alert.AlertType.WARNING, "Spreadsheet", "Invalid input", "Braces are not balanced");
            return false;
        } else if (!SyntaxAnalyzer.isBracesProperlyPositioned(tokensStream)) {
            DialogHelper.showAlert(Alert.AlertType.WARNING, "Spreadsheet", "Invalid input", "Braces incorrectly place");
            return false;
        }
        return true;
    }

    public Cell getCell(int row, int column) {
        return mGraph.getCell(row, column);
    }
}
