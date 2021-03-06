package kotlin.collections;

import androidx.exifinterface.media.ExifInterface;
import java.util.Iterator;
import kotlin.Metadata;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.internal.markers.KMappedMarker;

@Metadata(bv = {1, 0, 3}, d1 = {"\u0000\u001c\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u001c\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010(\n\u0002\b\u0003\b\u0000\u0018\u0000*\u0006\b\u0000\u0010\u0001 \u00012\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u0002H\u00010\u00030\u0002B\u0019\u0012\u0012\u0010\u0004\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00028\u00000\u00060\u0005¢\u0006\u0002\u0010\u0007J\u0015\u0010\b\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00028\u00000\u00030\u0006H\u0002R\u001a\u0010\u0004\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00028\u00000\u00060\u0005X\u0004¢\u0006\u0002\n\u0000¨\u0006\t"}, d2 = {"Lkotlin/collections/IndexingIterable;", ExifInterface.GPS_DIRECTION_TRUE, "", "Lkotlin/collections/IndexedValue;", "iteratorFactory", "Lkotlin/Function0;", "", "(Lkotlin/jvm/functions/Function0;)V", "iterator", "kotlin-stdlib"}, k = 1, mv = {1, 1, 16})
/* compiled from: Iterables.kt */
public final class IndexingIterable<T> implements Iterable<IndexedValue<? extends T>>, KMappedMarker {
    private final Function0<Iterator<T>> iteratorFactory;

    /* JADX WARN: Type inference failed for: r2v0, types: [kotlin.jvm.functions.Function0<? extends java.util.Iterator<? extends T>>, kotlin.jvm.functions.Function0<java.util.Iterator<T>>, java.lang.Object] */
    /* JADX WARNING: Unknown variable types count: 1 */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public IndexingIterable(kotlin.jvm.functions.Function0<? extends java.util.Iterator<? extends T>> r2) {
        /*
            r1 = this;
            java.lang.String r0 = "iteratorFactory"
            kotlin.jvm.internal.Intrinsics.checkParameterIsNotNull(r2, r0)
            r1.<init>()
            r1.iteratorFactory = r2
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: kotlin.collections.IndexingIterable.<init>(kotlin.jvm.functions.Function0):void");
    }

    public Iterator<IndexedValue<T>> iterator() {
        return new IndexingIterator<>(this.iteratorFactory.invoke());
    }
}
