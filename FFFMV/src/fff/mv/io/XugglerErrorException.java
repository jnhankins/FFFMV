/**
 * FFFMV - An application for creating music videos using flame fractals.
 * Copyright (C) 2015 Jeremiah N. Hankins
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */

package fff.mv.io;

import com.xuggle.xuggler.IError;
import java.io.IOException;

/**
 * {@code XugglerErrorException} wraps an {@link IError} return code object.
 * 
 * @author Jeremiah N. Hankins
 */
public class XugglerErrorException extends IOException {
    
    /**
     * The wrapped error.
     */
    private final IError error;
    
    /**
     * Constructs a new {@code XugglerErrorException} for the specified error
     * and message.
     * 
     * @param error the error
     * @param msg the message
     */
    public XugglerErrorException(IError error, String msg) {
        super(msg);
        this.error = error;
    }
    
    /**
     * Returns the wrapped error message.
     * 
     * @return the wrapped error message
     */
    public IError getError() {
        return error;
    }
    
    @Override
    public String toString() {
        if (error != null)
            return error.toString() + super.toString();
        return super.toString();
    }
}
