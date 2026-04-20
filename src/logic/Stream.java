package logic;

import java.util.List;

/**
 * CS 351L Project 4: Silos
 * @author Kiana Tarter and Neveah Martinez
 * Represents an input or output stream attached to the edge of the silo grid.
 */
public record Stream(String name, int row, int col, List<Integer> values) {
}
