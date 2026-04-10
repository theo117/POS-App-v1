import java.time.temporal.TemporalAccessor;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class JsonUtils
{
    private JsonUtils()
    {
    }

    public static String toJson(Object value)
    {
        if (value == null)
        {
            return "null";
        }
        if (value instanceof String)
        {
            return quote((String) value);
        }
        if (value instanceof Number || value instanceof Boolean)
        {
            return String.valueOf(value);
        }
        if (value instanceof TemporalAccessor)
        {
            return quote(String.valueOf(value));
        }
        if (value instanceof Map)
        {
            return mapToJson((Map<?, ?>) value);
        }
        if (value instanceof List)
        {
            return listToJson((List<?>) value);
        }

        return quote(String.valueOf(value));
    }

    private static String mapToJson(Map<?, ?> map)
    {
        StringBuilder builder = new StringBuilder();
        builder.append('{');

        Iterator<? extends Map.Entry<?, ?>> iterator = map.entrySet().iterator();
        while (iterator.hasNext())
        {
            Map.Entry<?, ?> entry = iterator.next();
            builder.append(quote(String.valueOf(entry.getKey())));
            builder.append(':');
            builder.append(toJson(entry.getValue()));
            if (iterator.hasNext())
            {
                builder.append(',');
            }
        }

        builder.append('}');
        return builder.toString();
    }

    private static String listToJson(List<?> list)
    {
        StringBuilder builder = new StringBuilder();
        builder.append('[');

        for (int index = 0; index < list.size(); index++)
        {
            if (index > 0)
            {
                builder.append(',');
            }
            builder.append(toJson(list.get(index)));
        }

        builder.append(']');
        return builder.toString();
    }

    private static String quote(String value)
    {
        StringBuilder builder = new StringBuilder();
        builder.append('"');

        for (int index = 0; index < value.length(); index++)
        {
            char character = value.charAt(index);
            switch (character)
            {
                case '\\':
                    builder.append("\\\\");
                    break;
                case '"':
                    builder.append("\\\"");
                    break;
                case '\b':
                    builder.append("\\b");
                    break;
                case '\f':
                    builder.append("\\f");
                    break;
                case '\n':
                    builder.append("\\n");
                    break;
                case '\r':
                    builder.append("\\r");
                    break;
                case '\t':
                    builder.append("\\t");
                    break;
                default:
                    if (character < 0x20)
                    {
                        builder.append(String.format("\\u%04x", (int) character));
                    }
                    else
                    {
                        builder.append(character);
                    }
                    break;
            }
        }

        builder.append('"');
        return builder.toString();
    }
}
