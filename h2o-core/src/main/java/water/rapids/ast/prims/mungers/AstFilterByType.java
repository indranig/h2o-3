package water.rapids.ast.prims.mungers;

import water.fvec.*;
import water.rapids.Env;
import water.rapids.Val;
import water.rapids.ast.AstPrimitive;
import water.rapids.ast.AstRoot;
import water.rapids.vals.ValNums;
import java.util.ArrayList;

/**
 * Filter an H2OFrame by col type
 *
 * Note: Current implementation is NOT in-place replacement
 */
public class AstFilterByType extends AstPrimitive {
    @Override
    public String[] args() {
        return new String[]{"ary","type"};
    }

    private enum Mode {Numeric,Categorical,String,Time,UUID,Bad}

    @Override
    public String str() {
        return "filterByType";
    }

    @Override
    public int nargs() {
        return 1 + 2;
    } //ary type

    @Override
    public Val apply(Env env, Env.StackHelp stk, AstRoot asts[]) {
        Frame fr = stk.track(asts[1].exec(env)).getFrame();
        String type = stk.track(asts[2].exec(env)).getStr();
        Mode mode;
        switch (type) {
            case "numeric": // Numeric, but not categorical or time
                mode = Mode.Numeric;
                break;
            case "categorical": // Integer, with a categorical/factor String mapping
                mode = Mode.Categorical;
                break;
            case "string": // String
                mode = Mode.String;
                break;
            case "time": // Long msec since the Unix Epoch - with a variety of display/parse options
                mode = Mode.Time;
                break;
            case "uuid": // UUID
                mode = Mode.UUID;
                break;
            case "bad": // No none-NA rows (triple negative! all NAs or zero rows)
                mode = Mode.Bad;
                break;
            default:
                throw new IllegalArgumentException("unknown data type to filter by: " + type);
        }
        Vec vecs[] = fr.vecs();
        ArrayList<Double> idxs = new ArrayList<>();
        for (double i = 0; i < fr.numCols(); i++)
            if (mode.equals(Mode.Numeric) && vecs[(int) i].isNumeric()){
                    idxs.add(i);
            }
            else if (mode.equals(Mode.Categorical) && vecs[(int) i].isCategorical()){
                idxs.add(i);
            }
            else if (mode.equals(Mode.String) && vecs[(int) i].isString()){
                idxs.add(i);
            }
            else if (mode.equals(Mode.Time) && vecs[(int) i].isTime()){
                idxs.add(i);
            }
            else if (mode.equals(Mode.UUID) && vecs[(int) i].isUUID()){
                idxs.add(i);
            } else if (mode.equals(Mode.Bad) && vecs[(int) i].isBad()){
                idxs.add(i);
            }

        double[] include_cols = new double[idxs.size()];
        int i = 0;
        for (double d : idxs)
            include_cols[i++] = (int) d;
        return new ValNums(include_cols);
    }
}

